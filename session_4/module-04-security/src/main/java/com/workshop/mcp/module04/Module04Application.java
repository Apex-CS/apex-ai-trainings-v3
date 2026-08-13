package com.workshop.mcp.module04;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Module 04 — Security: OAuth2, Guardrails and Audit Logging.
 *
 * <p>A Deployment MCP Server secured with:
 * <ul>
 *   <li>OAuth2 Bearer JWT validation (Spring Security + Keycloak)</li>
 *   <li>Human-in-the-loop for PROD deployments</li>
 *   <li>Prompt injection detection on all tool inputs</li>
 *   <li>Structured JSON audit logging</li>
 *   <li>Resilience4j rate limiting (10 calls/min per client)</li>
 * </ul>
 */
@SpringBootApplication
public class Module04Application {

    public static void main(String[] args) {
        SpringApplication.run(Module04Application.class, args);
    }
}
