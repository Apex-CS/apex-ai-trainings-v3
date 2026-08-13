package com.workshop.mcp.module05.agent;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.workshop.mcp.module05.dto.JiraIssueDTO;
import com.workshop.mcp.module05.dto.ReleaseResult;
import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.client.transport.HttpClientSseClientTransport;
import io.modelcontextprotocol.spec.McpSchema;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * Release Integration Agent — Module 05.
 *
 * <p>Orchestrates a release gate check across two MCP Servers:
 * <ol>
 *   <li>Jira MCP Server: check for critical open bugs (gate check)</li>
 *   <li>Secure Deployment MCP Server: trigger deployment if gate passes</li>
 * </ol>
 *
 * <p>The caller's OAuth2 Bearer token is relayed to the Deployment MCP Server
 * for authentication (Token Relay pattern).
 */
@Service
public class ReleaseIntegrationAgent {

    private static final Logger log = LoggerFactory.getLogger(ReleaseIntegrationAgent.class);

    @Value("${jira.mcp.server.url}")
    private String jiraMcpUrl;

    @Value("${deployment.mcp.server.url}")
    private String deploymentMcpUrl;

    private final ObjectMapper objectMapper;

    public ReleaseIntegrationAgent(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * Executes the full release gate flow.
     *
     * @param projectKey      Jira project key (e.g. "PROJ")
     * @param version         Release version (e.g. "3.0")
     * @param applicationName Application to deploy (e.g. "payment-service")
     * @param environment     Target environment (DEV, STAGING, PROD)
     * @param bearerToken     Caller's OAuth2 Bearer token — relayed to Deployment MCP Server
     */
    public ReleaseResult executeRelease(
            String projectKey, String version,
            String applicationName, String environment,
            String bearerToken) {

        log.info("=== Release Agent START: {}/{} → {}/{} ===",
                projectKey, version, applicationName, environment);

        // ─── Step 1: Jira critical bug check ─────────────────────────────────
        log.info("Step 1: Querying Jira MCP Server for critical bugs in {}/{}", projectKey, version);
        List<JiraIssueDTO> blockers = checkJiraForBlockers(projectKey, version);

        if (!blockers.isEmpty()) {
            log.warn("Step 1 FAILED: {} critical open bug(s) found — release BLOCKED", blockers.size());
            return ReleaseResult.blocked(projectKey, version, blockers);
        }

        log.info("Step 1 PASSED: No critical bugs found for {}/{}", projectKey, version);

        // ─── Step 2: Trigger deployment ───────────────────────────────────────
        log.info("Step 2: Triggering deployment of {} {} to {}", applicationName, version, environment);
        return triggerDeployment(applicationName, version, environment, bearerToken);
    }

    // ─── Private: Jira Gate Check ─────────────────────────────────────────────

    private List<JiraIssueDTO> checkJiraForBlockers(String projectKey, String version) {
        // Create a new client per request (Jira MCP Server is public, no auth required)
        try (McpSyncClient jiraClient = buildPublicClient(jiraMcpUrl)) {
            jiraClient.initialize();

            String jql = "project=%s AND priority=Critical AND issuetype=Bug AND fixVersion=\"%s\" AND status!=Done"
                    .formatted(projectKey, version);

            var request = new McpSchema.CallToolRequest(
                    "jira_search_issues",
                    Map.<String, Object>of("jql", jql, "maxResults", 50));

            var result = jiraClient.callTool(request);

            if (Boolean.TRUE.equals(result.isError())) {
                log.error("Jira search failed: {}", extractText(result));
                return List.of();  // Fail open — if Jira is down, don't block releases
            }

            String json = extractText(result);
            return objectMapper.readValue(json, new TypeReference<List<JiraIssueDTO>>() {});

        } catch (Exception e) {
            log.error("Jira MCP Client error: {}", e.getMessage(), e);
            return List.of();
        }
    }

    // ─── Private: Deployment Trigger ─────────────────────────────────────────

    private ReleaseResult triggerDeployment(
            String appName, String version, String environment, String bearerToken) {

        // Deployment MCP Server requires OAuth2 authentication (Module 04)
        try (McpSyncClient deploymentClient = buildAuthenticatedClient(deploymentMcpUrl, bearerToken)) {
            deploymentClient.initialize();

            var request = new McpSchema.CallToolRequest(
                    "triggerDeployment",
                    Map.<String, Object>of(
                            "applicationName", appName,
                            "version", version,
                            "environment", environment));

            var result = deploymentClient.callTool(request);
            String responseJson = extractText(result);

            if (Boolean.TRUE.equals(result.isError())) {
                return ReleaseResult.failed(responseJson);
            }

            JsonNode responseNode = objectMapper.readTree(responseJson);

            // Check if PROD requires human approval
            if ("PENDING_APPROVAL".equals(responseNode.path("status").asText())) {
                return ReleaseResult.pendingApproval(
                        responseNode.path("requestId").asText(),
                        responseNode.path("approvalUrl").asText(),
                        responseNode.path("message").asText());
            }

            return ReleaseResult.deployed(appName, version, environment, responseNode);

        } catch (Exception e) {
            log.error("Deployment MCP Client error: {}", e.getMessage(), e);
            return ReleaseResult.failed("Deployment error: " + e.getMessage());
        }
    }

    // ─── MCP Client factories ─────────────────────────────────────────────────

    private McpSyncClient buildPublicClient(String serverUrl) {
        var transport = HttpClientSseClientTransport.builder(serverUrl)
                .sseEndpoint("/sse")
                .build();
        return McpClient.sync(transport)
                .clientInfo(new McpSchema.Implementation("release-agent", "1.0.0"))
                .build();
    }

    private McpSyncClient buildAuthenticatedClient(String serverUrl, String bearerToken) {
        var transport = HttpClientSseClientTransport.builder(serverUrl)
                .sseEndpoint("/sse")
                // Token Relay: forward the caller's JWT to the downstream MCP Server
                .customizeRequest(builder -> builder
                        .header("Authorization", "Bearer " + bearerToken))
                .build();
        return McpClient.sync(transport)
                .clientInfo(new McpSchema.Implementation("release-agent", "1.0.0"))
                .build();
    }

    private String extractText(McpSchema.CallToolResult result) {
        return result.content().stream()
                .filter(c -> c instanceof McpSchema.TextContent)
                .map(c -> ((McpSchema.TextContent) c).text())
                .findFirst()
                .orElse("{}");
    }
}
