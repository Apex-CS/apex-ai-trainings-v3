package com.workshop.mcp.module05;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Module 05 — End-to-End Lab: Enterprise Release Integration Agent.
 *
 * <p>An agent that orchestrates two MCP Servers (Jira + Secure Deployment)
 * to automate enterprise release gate checks:
 * <ol>
 *   <li>Check Jira for critical open bugs (MCP Client → WireMock Jira)</li>
 *   <li>Block if bugs found; proceed if clear</li>
 *   <li>Trigger deployment (MCP Client → Secure Deployment MCP Server)</li>
 *   <li>Handle PENDING_APPROVAL for PROD with human-in-the-loop</li>
 * </ol>
 *
 * <p>Usage: POST /release with JSON body and Bearer token
 * <pre>
 * curl -X POST http://localhost:8085/release \
 *   -H 'Content-Type: application/json' \
 *   -H 'Authorization: Bearer $TOKEN' \
 *   -d '{"projectKey":"PROJ","version":"3.0","applicationName":"payment-service","environment":"PROD"}'
 * </pre>
 */
@SpringBootApplication
public class Module05Application {

    public static void main(String[] args) {
        SpringApplication.run(Module05Application.class, args);
    }
}
