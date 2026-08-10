package com.owasp.it.service;

import com.owasp.it.config.AppProperties;
import com.owasp.it.config.AppProperties.ManagedServerProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

@Component
public class JavaAppRestartExecutor {

    private static final Logger log = LoggerFactory.getLogger(JavaAppRestartExecutor.class);

    private final AppProperties appProperties;
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(3))
            .build();

    public JavaAppRestartExecutor(AppProperties appProperties) {
        this.appProperties = appProperties;
    }

    public void stopProcessOnPort(int port) {
        try {
            Process process = new ProcessBuilder("sh", "-c",
                    "lsof -ti:" + port + " | xargs kill -9 2>/dev/null || true")
                    .redirectErrorStream(true)
                    .start();
            process.waitFor(10, TimeUnit.SECONDS);
            Thread.sleep(1500);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            log.warn("Interrupted while stopping process on port {}", port);
        } catch (IOException ex) {
            log.warn("Failed to stop process on port {}: {}", port, ex.getMessage());
        }
    }

    public boolean startJavaApp(ManagedServerProperties server) {
        Path moduleDir = resolveModuleDir(server.moduleDir());
        if (!Files.isDirectory(moduleDir)) {
            log.warn("Module directory not found for {}: {}", server.appName(), moduleDir);
            return false;
        }

        try {
            ProcessBuilder builder = new ProcessBuilder(
                    "mvn", "-q", "spring-boot:run", "-Dspring-boot.run.workingDirectory=..");
            builder.directory(moduleDir.toFile());
            builder.redirectError(ProcessBuilder.Redirect.DISCARD);
            builder.redirectOutput(ProcessBuilder.Redirect.DISCARD);
            Process process = builder.start();
            log.info("Started restart process for {} (pid={})", server.appName(), process.pid());
            return true;
        } catch (IOException ex) {
            log.error("Failed to start {}: {}", server.appName(), ex.getMessage());
            return false;
        }
    }

    public boolean restartSelf(ManagedServerProperties server) {
        Path moduleDir = resolveModuleDir(server.moduleDir());
        if (!Files.isDirectory(moduleDir)) {
            log.warn("Module directory not found for self restart: {}", moduleDir);
            return false;
        }

        String command = String.format(
                "sleep 2; lsof -ti:%d | xargs kill -9 2>/dev/null || true; "
                        + "sleep 1; cd \"%s\" && mvn -q spring-boot:run -Dspring-boot.run.workingDirectory=.. "
                        + ">/dev/null 2>&1 &",
                server.port(), moduleDir.toAbsolutePath());

        try {
            Process process = new ProcessBuilder("sh", "-c", command)
                    .redirectErrorStream(true)
                    .start();
            boolean finished = process.waitFor(5, TimeUnit.SECONDS);
            log.info("Scheduled self-restart for {} (launcher finished={})", server.appName(), finished);
            return true;
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            return false;
        } catch (IOException ex) {
            log.error("Failed to schedule self-restart: {}", ex.getMessage());
            return false;
        }
    }

    public boolean waitForHealthy(String appHost, int timeoutSeconds) {
        String healthUrl = appHost.endsWith("/")
                ? appHost + "actuator/health"
                : appHost + "/actuator/health";
        long deadline = System.currentTimeMillis() + (timeoutSeconds * 1000L);

        while (System.currentTimeMillis() < deadline) {
            if (isHealthy(healthUrl)) {
                return true;
            }
            try {
                Thread.sleep(2000);
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                return false;
            }
        }
        return false;
    }

    private boolean isHealthy(String healthUrl) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(healthUrl))
                    .timeout(Duration.ofSeconds(3))
                    .GET()
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            return response.statusCode() == 200 && response.body().contains("\"status\":\"UP\"");
        } catch (Exception ex) {
            return false;
        }
    }

    private Path resolveModuleDir(String moduleDir) {
        return Path.of(appProperties.workspaceRoot()).resolve(moduleDir).normalize();
    }
}
