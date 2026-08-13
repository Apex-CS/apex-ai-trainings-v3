package com.workshop.mcp.module03;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Module 03 — Custom MCP Server: Customer API Wrapper.
 *
 * <p>This MCP Server wraps a legacy Customer REST API and exposes it to LLMs via:
 * <ul>
 *   <li><b>Tools:</b> createCustomer, getCustomer, listCustomers, updateCustomer</li>
 *   <li><b>Resources:</b> customers://all, schema://customer</li>
 *   <li><b>Prompts:</b> customer_support_response</li>
 * </ul>
 *
 * <p>Test with MCP Inspector:
 * <pre>
 *   npx @modelcontextprotocol/inspector --transport sse --url http://localhost:8083/sse
 * </pre>
 */
@SpringBootApplication
public class Module03Application {

    public static void main(String[] args) {
        SpringApplication.run(Module03Application.class, args);
    }
}
