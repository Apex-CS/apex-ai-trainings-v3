package com.workshop.mcp.module03.resources;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.workshop.mcp.module03.service.CustomerService;
import io.modelcontextprotocol.server.McpServerFeatures;
import io.modelcontextprotocol.server.McpSyncServerExchange;
import io.modelcontextprotocol.spec.McpSchema;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * MCP Resource Provider — Module 03.
 *
 * <p>Resources expose read-only context data to LLMs. Unlike Tools (which perform actions),
 * Resources are included in the LLM's context window so it understands the current state
 * of the system before deciding which tool to call.
 *
 * <p>Registered resources:
 * <ul>
 *   <li>{@code customers://all} — live list of all customers</li>
 *   <li>{@code schema://customer} — JSON Schema for the Customer entity</li>
 * </ul>
 */
@Configuration
public class CustomerResourceProvider {

    private final CustomerService customerService;
    private final ObjectMapper objectMapper;

    public CustomerResourceProvider(CustomerService customerService, ObjectMapper objectMapper) {
        this.customerService = customerService;
        this.objectMapper = objectMapper;
    }

    @Bean
    public List<McpServerFeatures.SyncResourceSpecification> customerResources() {
        return List.of(
                allCustomersResource(),
                customerSchemaResource()
        );
    }

    private McpServerFeatures.SyncResourceSpecification allCustomersResource() {
        var descriptor = new McpSchema.Resource(
                "customers://all",
                "All Customers",
                "Complete list of all customers in JSON format. Read-only context for LLMs.",
                "application/json",
                null);

        return new McpServerFeatures.SyncResourceSpecification(descriptor, (McpSyncServerExchange exchange, McpSchema.ReadResourceRequest req) -> {
            var customers = customerService.findAll(null, null);
            String json = serialize(customers);
            return new McpSchema.ReadResourceResult(
                    List.of(new McpSchema.TextResourceContents(
                            "customers://all", "application/json", json)));
        });
    }

    private McpServerFeatures.SyncResourceSpecification customerSchemaResource() {
        var descriptor = new McpSchema.Resource(
                "schema://customer",
                "Customer Entity Schema",
                "JSON Schema definition for the Customer entity. Use this to understand valid field values.",
                "application/json",
                null);

        String schema = """
                {
                  "$schema": "http://json-schema.org/draft-07/schema#",
                  "title": "Customer",
                  "type": "object",
                  "properties": {
                    "id":        { "type": "string", "readOnly": true },
                    "name":      { "type": "string", "minLength": 2, "maxLength": 100 },
                    "email":     { "type": "string", "format": "email" },
                    "phone":     { "type": "string", "pattern": "^\\\\+[1-9]\\\\d{1,14}$" },
                    "tier":      { "type": "string", "enum": ["STANDARD", "PREMIUM", "ENTERPRISE"] },
                    "status":    { "type": "string", "enum": ["ACTIVE", "INACTIVE", "SUSPENDED"], "readOnly": true },
                    "createdAt": { "type": "string", "format": "date-time", "readOnly": true },
                    "updatedAt": { "type": "string", "format": "date-time", "readOnly": true }
                  },
                  "required": ["name", "email", "phone", "tier"]
                }""";

        return new McpServerFeatures.SyncResourceSpecification(descriptor, (McpSyncServerExchange exchange, McpSchema.ReadResourceRequest req) ->
                new McpSchema.ReadResourceResult(
                        List.of(new McpSchema.TextResourceContents(
                                "schema://customer", "application/json", schema))));
    }

    private String serialize(Object obj) {
        try {
            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(obj);
        } catch (Exception e) {
            return "[]";
        }
    }
}
