package com.owasp.aiassistant.tools;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.owasp.aiassistant.corporate.client.CorporateApiClient;
import com.owasp.aiassistant.corporate.enums.AppServerName;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class ItApiTools {

    private static final String LIST_SERVERS_TOOL = "listAppServers";
    private static final String RESTART_SERVER_TOOL = "restartAppServer";
    private static final String LIST_RESTARTS_TOOL = "listAppRestartsByApp";

    private final CorporateApiClient corporateApiClient;
    private final ObjectMapper objectMapper;

    public ItApiTools(CorporateApiClient corporateApiClient, ObjectMapper objectMapper) {
        this.corporateApiClient = corporateApiClient;
        this.objectMapper = objectMapper;
    }

    @Tool(description = """
            List registered application servers from the IT operations API.
            Requires the chat user to have it-admin or it-user role.
            Returns app_name, app_host, and owner_area for financial-backend, it-backend, \
            sales-backend, and marketing-backend.
            """)
    public String listAppServers() {
        return corporateApiClient.get(
                LIST_SERVERS_TOOL,
                corporateApiClient.itBaseUrl(),
                "/api/list-app-servers",
                Map.of());
    }

    @Tool(description = """
            Restart a registered Java application server through the IT operations API.
            Requires the chat user to have it-admin role.
            Use AppServerName enum values: financial-backend, it-backend, sales-backend, marketing-backend.
            Records the restart attempt in app_restarts.
            """)
    public String restartAppServer(
            @ToolParam(description = "Application server name")
            AppServerName appName) {
        Map<String, String> body = Map.of("appName", appName.apiValue());

        try {
            return corporateApiClient.post(
                    RESTART_SERVER_TOOL,
                    corporateApiClient.itBaseUrl(),
                    "/api/restart-server",
                    objectMapper.writeValueAsString(body));
        } catch (JsonProcessingException e) {
            return "Failed to build restart request: " + e.getMessage();
        }
    }

    @Tool(description = """
            List restart attempts for a specific application from the IT operations API.
            Requires the chat user to have it-admin role.
            Use AppServerName enum values: financial-backend, it-backend, sales-backend, marketing-backend.
            """)
    public String listAppRestartsByApp(
            @ToolParam(description = "Application server name")
            AppServerName appName) {
        Map<String, String> queryParams = new LinkedHashMap<>();
        queryParams.put("appName", appName.apiValue());

        return corporateApiClient.get(
                LIST_RESTARTS_TOOL,
                corporateApiClient.itBaseUrl(),
                "/api/list-app-restarts-by-app",
                queryParams);
    }
}
