package com.owasp.it.service;

import com.owasp.it.config.AppProperties;
import com.owasp.it.config.AppProperties.ManagedServerProperties;
import com.owasp.it.domain.AppRestart;
import com.owasp.it.dto.AppRestartResponse;
import com.owasp.it.dto.AppServerResponse;
import com.owasp.it.dto.RestartServerRequest;
import com.owasp.it.repository.AppRestartRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class ItOperationsService {

    private static final Logger log = LoggerFactory.getLogger(ItOperationsService.class);
    private static final String SELF_APP_NAME = "it-backend";

    private final AppProperties appProperties;
    private final AppRestartRepository appRestartRepository;
    private final JavaAppRestartExecutor restartExecutor;

    public ItOperationsService(
            AppProperties appProperties,
            AppRestartRepository appRestartRepository,
            JavaAppRestartExecutor restartExecutor) {
        this.appProperties = appProperties;
        this.appRestartRepository = appRestartRepository;
        this.restartExecutor = restartExecutor;
    }

    public List<AppServerResponse> listAppServers() {
        return appProperties.servers().stream()
                .map(server -> new AppServerResponse(server.appName(), server.appHost(), server.ownerArea()))
                .toList();
    }

    @Transactional
    public AppRestartResponse restartServer(RestartServerRequest request, String username) {
        ManagedServerProperties server = findServer(request.appName());

        AppRestart restart = new AppRestart();
        restart.setAppName(server.appName());
        restart.setUserRequested(username);
        restart.setOperationDate(LocalDateTime.now());
        restart.setOperationDone(false);
        restart = appRestartRepository.save(restart);

        boolean success;
        if (SELF_APP_NAME.equalsIgnoreCase(server.appName())) {
            success = restartExecutor.restartSelf(server);
        } else {
            restartExecutor.stopProcessOnPort(server.port());
            success = restartExecutor.startJavaApp(server)
                    && restartExecutor.waitForHealthy(server.appHost(), appProperties.restart().startupTimeoutSeconds());
        }

        restart.setOperationDone(success);
        restart = appRestartRepository.save(restart);

        if (!success) {
            log.warn("Restart failed for app {} requested by {}", server.appName(), username);
        }

        return toResponse(restart);
    }

    @Transactional(readOnly = true)
    public List<AppRestartResponse> listAppRestartsByApp(String appName) {
        findServer(appName);
        return appRestartRepository.findByAppNameIgnoreCaseOrderByOperationDateDesc(appName).stream()
                .map(this::toResponse)
                .toList();
    }

    private ManagedServerProperties findServer(String appName) {
        return appProperties.servers().stream()
                .filter(server -> server.appName().equalsIgnoreCase(appName))
                .findFirst()
                .orElseThrow(() -> new AppServerNotFoundException("Unknown app server: " + appName));
    }

    private AppRestartResponse toResponse(AppRestart restart) {
        return new AppRestartResponse(
                restart.getId(),
                restart.getAppName(),
                restart.getUserRequested(),
                restart.getOperationDate(),
                restart.isOperationDone());
    }
}
