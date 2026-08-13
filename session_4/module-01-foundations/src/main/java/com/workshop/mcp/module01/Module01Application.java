package com.workshop.mcp.module01;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Module 01 — Foundations: Hello MCP World
 *
 * <p>This application runs as a stdio MCP Server. It reads JSON-RPC 2.0 messages
 * from stdin and writes responses to stdout. It is designed to be launched by
 * MCP Inspector or any MCP Client that supports the stdio transport.
 *
 * <p>Launch command (via MCP Inspector):
 * <pre>
 *   npx @modelcontextprotocol/inspector \
 *     --transport stdio \
 *     --command 'java' \
 *     --args '-jar,target/module-01-foundations-1.0.0-SNAPSHOT.jar'
 * </pre>
 */
@SpringBootApplication
public class Module01Application {

    public static void main(String[] args) {
        SpringApplication.run(Module01Application.class, args);
    }
}
