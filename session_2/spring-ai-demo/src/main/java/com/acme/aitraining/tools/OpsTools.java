package com.acme.aitraining.tools;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * STEP 4 — Tools: let the model call YOUR Java code.
 *
 * Teaching points:
 *  - The model never executes anything. It asks; Spring AI invokes the method,
 *    returns the result to the model, and the model writes the final answer.
 *  - Descriptions are the API contract with the model: write them like docs.
 *  - Watch the round trip in the logs (two model calls).
 */
@Component
public class OpsTools {

    // Fake internal systems - replace with real service clients in production
    private static final Map<String, String> STATUS = Map.of(
            "payments", "DEGRADED - p99 latency 4.2s since 09:15, error rate 7%",
            "checkout", "HEALTHY",
            "inventory", "HEALTHY",
            "notifications", "DOWN - queue consumer crash-looping"
    );

    private static final Map<String, String> ONCALL = Map.of(
            "payments", "Sofia (ext 4411)",
            "checkout", "Diego (ext 4207)",
            "inventory", "Diego (ext 4207)",
            "notifications", "Karla (ext 4155)"
    );

    @Tool(description = "Get the current operational status of an internal service. " +
            "Valid services: payments, checkout, inventory, notifications.")
    public String serviceStatus(@ToolParam(description = "service name") String service) {
        return STATUS.getOrDefault(service.toLowerCase(), "UNKNOWN SERVICE");
    }

    @Tool(description = "Get the on-call engineer for an internal service.")
    public String onCallEngineer(@ToolParam(description = "service name") String service) {
        return ONCALL.getOrDefault(service.toLowerCase(), "No on-call configured");
    }
}
