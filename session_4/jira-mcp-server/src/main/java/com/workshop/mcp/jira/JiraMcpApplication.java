package com.workshop.mcp.jira;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Jira MCP Server — mock for Module 05 end-to-end lab.
 *
 * <p>Exposes {@code jira_search_issues} and {@code jira_get_issue} tools
 * over the Spring AI MCP HTTP+SSE transport on port 9001.
 */
@SpringBootApplication
public class JiraMcpApplication {

    public static void main(String[] args) {
        SpringApplication.run(JiraMcpApplication.class, args);
    }
}
