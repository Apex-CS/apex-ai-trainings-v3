package com.workshop.mcp.module02.client;

import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.workshop.mcp.module02.dto.JiraIssueDTO;

import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.spec.McpSchema;

/**
 * Jira MCP Client Service — Module 02.
 *
 * <p>Demonstrates three core patterns against the Jira MCP server (SSE transport):
 * <ol>
 *   <li><b>Tool Discovery</b> — {@code tools/list}: enumerate available Jira tools</li>
 *   <li><b>Typed Invocation</b> — fetch a Jira issue by key, deserialize to DTO</li>
 *   <li><b>JQL Search</b> — search for critical bugs, deserialize list of DTOs</li>
 * </ol>
 */
@Service
public class JiraMcpClientService {

    private static final Logger log = LoggerFactory.getLogger(JiraMcpClientService.class);

    private final McpSyncClient jiraMcpClient;
    private final ObjectMapper objectMapper;

    public JiraMcpClientService(McpSyncClient jiraMcpClient, ObjectMapper objectMapper) {
        this.jiraMcpClient = jiraMcpClient;
        this.objectMapper = objectMapper;
    }

    // ─── Pattern 1: Tool Discovery ────────────────────────────────────────────

    public List<McpSchema.Tool> listAvailableTools() {
        var result = jiraMcpClient.listTools(null);
        log.info("Server exposes {} tool(s): {}",
                result.tools().size(),
                result.tools().stream().map(McpSchema.Tool::name).toList());
        return result.tools();
    }

    // ─── Pattern 2: Typed Tool Invocation ─────────────────────────────────────

    public JiraIssueDTO getIssue(String issueKey) {
        log.debug("Calling jira_get_issue({})", issueKey);
        var result = jiraMcpClient.callTool(
                new McpSchema.CallToolRequest("jira_get_issue", Map.of("issueKey", issueKey)));

        if (Boolean.TRUE.equals(result.isError())) {
            throw new JiraMcpException("Failed to fetch issue " + issueKey + ": " + extractText(result));
        }
        return deserialize(extractText(result), JiraIssueDTO.class);
    }

    // ─── Pattern 3: JQL Search → List<DTO> ───────────────────────────────────

    public List<JiraIssueDTO> searchCriticalBugs(String projectKey, String fixVersion) {
        String jql = "project=%s AND priority=Critical AND issuetype=Bug AND fixVersion=\"%s\" AND status!=Done"
                .formatted(projectKey, fixVersion);
        log.debug("Calling jira_search_issues(jql={})", jql);
        var result = jiraMcpClient.callTool(
                new McpSchema.CallToolRequest("jira_search_issues",
                        Map.of("jql", jql, "maxResults", 50)));

        if (Boolean.TRUE.equals(result.isError())) {
            throw new JiraMcpException("Search failed: " + extractText(result));
        }
        return deserializeList(extractText(result));
    }

    // ─── Private helpers ──────────────────────────────────────────────────────

    private String extractText(McpSchema.CallToolResult result) {
        return result.content().stream()
                .filter(c -> c instanceof McpSchema.TextContent)
                .map(c -> ((McpSchema.TextContent) c).text())
                .findFirst()
                .orElse("");
    }

    private <T> T deserialize(String json, Class<T> type) {
        try {
            com.fasterxml.jackson.databind.JsonNode root = objectMapper.readTree(json);
            String actualJson = root.isTextual() ? root.asText() : json;
            return objectMapper.readValue(actualJson, type);
        } catch (Exception e) {
            throw new JiraMcpException("Failed to parse tool response as "
                    + type.getSimpleName() + ": " + e.getMessage());
        }
    }

    private List<JiraIssueDTO> deserializeList(String json) {
        try {
            com.fasterxml.jackson.databind.JsonNode root = objectMapper.readTree(json);
            String actualJson = root.isTextual() ? root.asText() : json;
            var type = objectMapper.getTypeFactory()
                    .constructCollectionType(List.class, JiraIssueDTO.class);
            return objectMapper.readValue(actualJson, type);
        } catch (Exception e) {
            throw new JiraMcpException("Failed to parse tool response as List<JiraIssueDTO>: " + e.getMessage());
        }
    }

    // ─── Exception ────────────────────────────────────────────────────────────

    public static class JiraMcpException extends RuntimeException {
        public JiraMcpException(String message) { super(message); }
    }
}
