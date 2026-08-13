package com.workshop.mcp.module02;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Module 02 — MCP Client: Consuming External MCP Servers.
 *
 * <p>Demonstrates how a Java service acts as an MCP Client, connecting to an
 * external Jira MCP Server over SSE transport, discovering its tools at startup,
 * and invoking them to query Jira issues.
 */
@SpringBootApplication
public class Module02Application {

    public static void main(String[] args) {
        SpringApplication.run(Module02Application.class, args);
    }
}
