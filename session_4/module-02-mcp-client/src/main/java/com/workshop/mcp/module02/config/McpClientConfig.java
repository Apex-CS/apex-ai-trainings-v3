package com.workshop.mcp.module02.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.client.transport.HttpClientSseClientTransport;
import io.modelcontextprotocol.spec.McpSchema;

/**
 * MCP Client Configuration — Module 02.
 *
 * <p>Creates a {@link McpSyncClient} connected to the Jira mock MCP server via
 * <b>SSE transport</b>. SSE works across network boundaries and supports
 * multiple concurrent clients — required for microservices.
 *
 * <p>{@link HttpClientSseClientTransport} establishes a persistent SSE connection
 * (GET /sse) and sends/receives all JSON-RPC messages as SSE events on that connection.
 * This is the standard MCP protocol for network-based communication.
 */
@Configuration
public class McpClientConfig {

    @Value("${jira.mcp.server.url}")
    private String jiraMcpServerUrl;

    @Bean(destroyMethod = "close")
    public McpSyncClient jiraMcpClient() {
        var transport = HttpClientSseClientTransport.builder(jiraMcpServerUrl)
                .sseEndpoint("/sse")
                .build();

        var client = McpClient.sync(transport)
                .clientInfo(new McpSchema.Implementation("workshop-jira-client", "1.0.0"))
                .build();

        // MCP handshake: sends initialize request, receives server capabilities
        client.initialize();
        return client;
    }
}
