package com.workshop.mcp.module04.tools;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.workshop.mcp.module04.audit.AuditLogService;
import com.workshop.mcp.module04.security.CallerIdentity;
import com.workshop.mcp.module04.security.HumanInTheLoopGuard;
import com.workshop.mcp.module04.security.InputSanitizer;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.Map;

/**
 * Secure Deployment MCP Tools — Module 04.
 *
 * <p>Demonstrates four security layers applied to every tool call:
 * <ol>
 *   <li><b>Authentication</b>: Spring Security validates the JWT before this method is reached</li>
 *   <li><b>Rate Limiting</b>: @RateLimiter prevents runaway LLM loops (10 calls/60s)</li>
 *   <li><b>Input Sanitization</b>: all @ToolParam values checked for injection patterns</li>
 *   <li><b>Human-in-the-Loop</b>: PROD deployments require explicit human approval</li>
 * </ol>
 */
@Service
public class SecureDeploymentTools {

    private static final Logger log = LoggerFactory.getLogger(SecureDeploymentTools.class);

    private final HumanInTheLoopGuard humanGuard;
    private final InputSanitizer sanitizer;
    private final AuditLogService auditLog;
    private final RestClient deploymentClient;
    private final ObjectMapper objectMapper;

    public SecureDeploymentTools(
            HumanInTheLoopGuard humanGuard,
            InputSanitizer sanitizer,
            AuditLogService auditLog,
            @Value("${deployment.api.base-url}") String deploymentApiUrl,
            ObjectMapper objectMapper) {
        this.humanGuard = humanGuard;
        this.sanitizer = sanitizer;
        this.auditLog = auditLog;
        this.objectMapper = objectMapper;
        this.deploymentClient = RestClient.builder()
                .baseUrl(deploymentApiUrl)
                // Force HTTP/1.1 — WireMock does not support HTTP/2 upgrade over plain HTTP
                .requestFactory(new SimpleClientHttpRequestFactory())
                .defaultHeader("Accept", MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    @Tool(description = """
            Triggers a deployment of the specified application version to an environment.
            Valid environments: DEV, STAGING, PROD.
            IMPORTANT: Deployments to PROD require explicit human approval.
            For PROD, this tool returns a PENDING_APPROVAL status with an approvalUrl.
            A human must visit the approvalUrl to authorize the deployment before it executes.""")
    @RateLimiter(name = "mcp-tool-calls", fallbackMethod = "rateLimitFallback")
    public String triggerDeployment(
            @ToolParam(description = "Application name to deploy, e.g. 'payment-service'") String applicationName,
            @ToolParam(description = "Version tag, e.g. 'v2.4.1'") String version,
            @ToolParam(description = "Target environment: DEV, STAGING, or PROD") String environment) {

        // Layer 1: Extract caller identity from JWT (authentication already done by Spring Security)
        var identity = CallerIdentity.fromSecurityContext();

        // Layer 2: Audit — log intent BEFORE executing (compliance requirement)
        auditLog.toolInvoked("triggerDeployment", identity,
                Map.of("applicationName", applicationName, "version", version, "environment", environment));

        // Layer 3: Input sanitization — reject prompt injection attempts
        try {
            sanitizer.assertSafe(applicationName, "applicationName");
            sanitizer.assertSafe(version, "version");
            sanitizer.assertSafe(environment, "environment");
        } catch (InputSanitizer.PromptInjectionException e) {
            auditLog.injectionDetected("triggerDeployment", identity, e.getMessage());
            // Re-throw as a JSON-formatted RuntimeException so MCP sets isError: true
            throw new RuntimeException(toErrorJson(e.getMessage()));
        }

        // Layer 4: Human-in-the-loop for PROD deployments
        if ("PROD".equalsIgnoreCase(environment)) {
            String requestId = humanGuard.requireApproval(
                    "Deploy %s %s to PROD".formatted(applicationName, version),
                    identity.username());
            auditLog.approvalRequired("triggerDeployment", identity, requestId);

            return toJson(Map.of(
                    "status", "PENDING_APPROVAL",
                    "message", "PROD deployment requires human approval. Please visit the approval URL.",
                    "requestId", requestId,
                    "approvalUrl", "/confirm/" + requestId,
                    "requestedBy", identity.username()));
        }

        // Execute deployment — forward caller's token to downstream API (Token Relay)
        try {
            String result = deploymentClient.post()
                    .uri("/api/deployments")
                    .header("Authorization", "Bearer " + identity.token())
                    .body(Map.of(
                            "applicationName", applicationName,
                            "version", version,
                            "environment", environment))
                    .retrieve()
                    .body(String.class);

            auditLog.toolCompleted("triggerDeployment", identity, "SUCCESS");
            return result;
        } catch (Exception e) {
            auditLog.toolFailed("triggerDeployment", identity, e.getClass().getSimpleName(), e.getMessage());
            return toErrorJson("Deployment API error: " + e.getMessage());
        }
    }

    @Tool(description = "Returns the current deployment status for the specified application and environment.")
    @RateLimiter(name = "mcp-tool-calls", fallbackMethod = "rateLimitFallback")
    public String getDeploymentStatus(
            @ToolParam(description = "Application name") String applicationName,
            @ToolParam(description = "Environment: DEV, STAGING, or PROD") String environment) {

        var identity = CallerIdentity.fromSecurityContext();
        auditLog.toolInvoked("getDeploymentStatus", identity,
                Map.of("applicationName", applicationName, "environment", environment));

        sanitizer.assertSafe(applicationName, "applicationName");
        sanitizer.assertSafe(environment, "environment");

        String result = deploymentClient.get()
                .uri("/api/deployments/{app}/{env}", applicationName, environment)
                .header("Authorization", "Bearer " + identity.token())
                .retrieve()
                .body(String.class);

        auditLog.toolCompleted("getDeploymentStatus", identity, "SUCCESS");
        return result;
    }

    // ─── Resilience4j fallback — called when rate limit is exceeded ───────────
    // Note: Resilience4j also calls this fallback for ANY exception thrown from the method.
    // We check the exception type to distinguish rate limiting from security exceptions.

    public String rateLimitFallback(String a, String b, String c, Throwable t) {
        if (t instanceof io.github.resilience4j.ratelimiter.RequestNotPermitted) {
            var identity = CallerIdentity.fromSecurityContext();
            auditLog.rateLimitExceeded("triggerDeployment", identity);
            return toErrorJson("Rate limit exceeded. Maximum 10 tool calls per 60 seconds. Please wait and retry.");
        }
        // Re-throw non-rate-limit exceptions so the MCP framework sets isError: true
        if (t instanceof RuntimeException re) throw re;
        throw new RuntimeException(t);
    }

    public String rateLimitFallback(String a, String b, Throwable t) {
        if (t instanceof io.github.resilience4j.ratelimiter.RequestNotPermitted) {
            var identity = CallerIdentity.fromSecurityContext();
            auditLog.rateLimitExceeded("getDeploymentStatus", identity);
            return toErrorJson("Rate limit exceeded. Please wait and retry.");
        }
        if (t instanceof RuntimeException re) throw re;
        throw new RuntimeException(t);
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    private String toJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (Exception e) {
            return "{\"error\": \"Serialization failed\"}";
        }
    }

    private String toErrorJson(String message) {
        return toJson(Map.of("error", message));
    }
}
