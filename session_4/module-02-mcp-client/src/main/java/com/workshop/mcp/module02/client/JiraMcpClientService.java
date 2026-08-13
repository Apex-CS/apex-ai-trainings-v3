package com.workshop.mcp.module02.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.workshop.mcp.module02.dto.SystemInfoDTO;
import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.spec.McpSchema;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * MCP Client Demo Service — Module 02.
 *
 * <p>Demonstrates the four core MCP client patterns against the Module 01 MCP server
 * (connected via stdio transport). The tool names and response shapes come from the
 * server — the client discovers them dynamically at runtime.
 *
 * <ol>
 *   <li><b>Tool Discovery</b> — {@code tools/list}: enumerate what the server offers</li>
 *   <li><b>Typed Invocation</b> — {@code tools/call} with arguments, parse numeric result</li>
 *   <li><b>Structured Response</b> — call {@code systemInfo}, deserialize JSON → DTO</li>
 *   <li><b>Error Handling</b> — detect {@code isError:true} and surface as Java exception</li>
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

    /**
     * Lists all tools available on the remote server.
     * No hardcoded tool names — fully dynamic discovery over JSON-RPC.
     *
     * <p>JSON-RPC exchange:
     * <pre>
     * → { "method": "tools/list", "params": {} }
     * ← { "result": { "tools": [ { "name": "add", ... }, ... ] } }
     * </pre>
     */
    public List<McpSchema.Tool> listAvailableTools() {
        var result = jiraMcpClient.listTools(null);
        log.info("Server exposes {} tool(s): {}",
                result.tools().size(),
                result.tools().stream().map(McpSchema.Tool::name).toList());
        return result.tools();
    }

    // ─── Pattern 2: Typed Tool Invocation ─────────────────────────────────────

    /**
     * Calls the {@code add} tool.
     *
     * <p>JSON-RPC exchange:
     * <pre>
     * → { "method": "tools/call", "params": { "name": "add", "arguments": { "a": 7, "b": 3 } } }
     * ← { "result": { "content": [{ "type": "text", "text": "10.0" }], "isError": false } }
     * </pre>
     */
    public double add(double a, double b) {
        log.debug("Calling add({}, {})", a, b);
        var result = jiraMcpClient.callTool(
                new McpSchema.CallToolRequest("add", Map.of("a", a, "b", b)));

        if (Boolean.TRUE.equals(result.isError())) {
            throw new JiraMcpException("add tool error: " + extractText(result));
        }
        return Double.parseDouble(extractText(result));
    }

    // ─── Pattern 3: Structured JSON Response → DTO ────────────────────────────

    /**
     * Calls {@code systemInfo} and deserializes the JSON response to a typed DTO.
     *
     * <p>JSON-RPC exchange:
     * <pre>
     * → { "method": "tools/call", "params": { "name": "systemInfo", "arguments": {} } }
     * ← { "result": { "content": [{ "type": "text", "text": "{\"javaVersion\":\"21\",...}" }] } }
     * </pre>
     */
    public SystemInfoDTO getSystemInfo() {
        log.debug("Calling systemInfo()");
        var result = jiraMcpClient.callTool(
                new McpSchema.CallToolRequest("systemInfo", Map.of()));

        if (Boolean.TRUE.equals(result.isError())) {
            throw new JiraMcpException("systemInfo tool error: " + extractText(result));
        }
        String json = extractText(result);
        log.debug("systemInfo response: {}", json);
        return deserialize(json, SystemInfoDTO.class);
    }

    // ─── Pattern 4: Error Handling (isError: true) ────────────────────────────

    /**
     * Calls the {@code divide} tool. When {@code b == 0} the server returns
     * {@code isError:true} instead of throwing — MCP tools signal errors in-band.
     *
     * <p>JSON-RPC error exchange:
     * <pre>
     * → { "method": "tools/call", "params": { "name": "divide", "arguments": { "dividend": 10, "divisor": 0 } } }
     * ← { "result": { "content": [{ "type": "text", "text": "Error: ..." }], "isError": true } }
     * </pre>
     */
    public double divide(double a, double b) {
        log.debug("Calling divide({}, {})", a, b);
        var result = jiraMcpClient.callTool(
                new McpSchema.CallToolRequest("divide", Map.of("dividend", a, "divisor", b)));

        if (Boolean.TRUE.equals(result.isError())) {
            String errorText = extractText(result);
            log.warn("Tool returned isError:true — {}", errorText);
            throw new JiraMcpException(errorText);
        }
        return Double.parseDouble(extractText(result));
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
            // Spring AI serializes String-returning tools as JSON strings ("..."),
            // so we may need to unwrap one extra encoding layer first.
            com.fasterxml.jackson.databind.JsonNode root = objectMapper.readTree(json);
            String actualJson = root.isTextual() ? root.asText() : json;
            return objectMapper.readValue(actualJson, type);
        } catch (Exception e) {
            throw new JiraMcpException("Failed to parse tool response as "
                    + type.getSimpleName() + ": " + e.getMessage());
        }
    }

    // ─── Exception ────────────────────────────────────────────────────────────

    public static class JiraMcpException extends RuntimeException {
        public JiraMcpException(String message) { super(message); }
    }
}
