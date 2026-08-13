package com.workshop.mcp.module02.config;

import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.client.transport.ServerParameters;
import io.modelcontextprotocol.client.transport.StdioClientTransport;
import io.modelcontextprotocol.spec.McpSchema;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * MCP Client Configuration — Module 02.
 *
 * <p>Creates a {@link McpSyncClient} connected to the Module 01 server via
 * <b>stdio transport</b>. Stdio launches the server as a child process and
 * communicates over its stdin/stdout pipes — the same mechanism used by
 * Claude Desktop and most real-world MCP hosts.
 *
 * <p>Stdio transport lifecycle:
 * <ol>
 *   <li>Client spawns the server process (java -jar module-01.jar)</li>
 *   <li>Client writes JSON-RPC requests to the process stdin</li>
 *   <li>Server writes JSON-RPC responses to stdout</li>
 *   <li>Client reads the responses from the process stdout</li>
 * </ol>
 */
@Configuration
public class McpClientConfig {

    @Value("${mcp.server.jar-path}")
    private String serverJarPath;

    @Bean(destroyMethod = "close")
    public McpSyncClient jiraMcpClient() {
        var serverParams = ServerParameters.builder("java")
                .args("-jar", serverJarPath)
                .build();

        var transport = new StdioClientTransport(serverParams);
        // Module 01 routes all logs to /tmp/module01-mcp-server.log, so suppress stderr here
        transport.setStdErrorHandler(line -> {});

        var client = McpClient.sync(transport)
                .clientInfo(new McpSchema.Implementation("workshop-mcp-client", "1.0.0"))
                .capabilities(McpSchema.ClientCapabilities.builder().build())
                .build();

        // MCP initialization handshake (sent over stdio, same protocol as SSE/HTTP)
        client.initialize();

        return client;
    }
}
