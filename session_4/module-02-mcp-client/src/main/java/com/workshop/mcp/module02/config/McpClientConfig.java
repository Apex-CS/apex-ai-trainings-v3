package com.workshop.mcp.module02.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.spec.McpSchema;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * MCP Client Configuration — Module 02.
 *
 * <p>Creates a {@link McpSyncClient} connected to the Jira mock MCP server via
 * <b>SSE transport</b>. SSE works across network boundaries and supports
 * multiple concurrent clients — required for microservices.
 *
 * <p>{@link HttpPostMcpTransport} performs the SSE endpoint discovery handshake
 * (GET /sse) then sends each JSON-RPC message via HTTP POST and reads the
 * response from the HTTP body — matching how WireMock serves the mock.
 */
@Configuration
public class McpClientConfig {

    @Value("${jira.mcp.server.url}")
    private String jiraMcpServerUrl;

    @Bean(destroyMethod = "close")
    public McpSyncClient jiraMcpClient(ObjectMapper objectMapper) {
        var transport = new HttpPostMcpTransport(jiraMcpServerUrl, "/sse", objectMapper);

        var client = McpClient.sync(transport)
                .clientInfo(new McpSchema.Implementation("workshop-jira-client", "1.0.0"))
                .build();

        // MCP handshake: sends initialize request, receives server capabilities
        client.initialize();
        return client;
    }
}
