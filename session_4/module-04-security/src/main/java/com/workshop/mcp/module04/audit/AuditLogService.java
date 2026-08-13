package com.workshop.mcp.module04.audit;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.workshop.mcp.module04.security.CallerIdentity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Structured JSON Audit Log Service — Module 04.
 *
 * <p>Every tool invocation is audited before and after execution.
 * Audit events are emitted to a dedicated 'AUDIT' logger (routed to a separate
 * file/stream in application.yml) for SIEM ingestion.
 *
 * <p>Security rules for audit logs:
 * <ul>
 *   <li>NEVER log token values (Bearer tokens, API keys, passwords)</li>
 *   <li>ALWAYS log the caller's identity (sub, email) — not the token itself</li>
 *   <li>ALWAYS log the tool name and arguments (redacting secrets)</li>
 *   <li>ALWAYS log the outcome (SUCCESS, FAILURE, PENDING_APPROVAL)</li>
 * </ul>
 */
@Service
public class AuditLogService {

    /** Dedicated audit logger — routes to a separate appender in logback config */
    private static final Logger AUDIT = LoggerFactory.getLogger("AUDIT");

    /** Field names that may contain sensitive values and should be redacted */
    private static final Set<String> SECRET_FIELD_NAMES = Set.of(
            "token", "secret", "password", "apikey", "api_key",
            "credential", "privatekey", "private_key", "accesstoken", "access_token"
    );

    private final ObjectMapper objectMapper;

    public AuditLogService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public void toolInvoked(String toolName, CallerIdentity caller, Map<String, Object> arguments) {
        emit("TOOL_INVOKED", toolName, caller, Map.of(
                "arguments", sanitizeArguments(arguments),
                "threadVirtual", Thread.currentThread().isVirtual(),
                "threadName", Thread.currentThread().getName()));
    }

    public void toolCompleted(String toolName, CallerIdentity caller, String outcome) {
        emit("TOOL_COMPLETED", toolName, caller, Map.of("outcome", outcome));
    }

    public void toolFailed(String toolName, CallerIdentity caller, String errorType, String errorMessage) {
        emit("TOOL_FAILED", toolName, caller, Map.of(
                "errorType", errorType,
                "errorMessage", errorMessage));
    }

    public void approvalRequired(String toolName, CallerIdentity caller, String requestId) {
        emit("APPROVAL_REQUIRED", toolName, caller, Map.of("requestId", requestId));
    }

    public void approvalGranted(String requestId, String approvedBy) {
        var event = buildBaseEvent("APPROVAL_GRANTED", "n/a", null);
        event.put("requestId", requestId);
        event.put("approvedBy", approvedBy);
        writeEvent(event);
    }

    public void injectionDetected(String toolName, CallerIdentity caller, String fieldName) {
        emit("INJECTION_DETECTED", toolName, caller, Map.of(
                "field", fieldName,
                "severity", "HIGH"));
    }

    public void rateLimitExceeded(String toolName, CallerIdentity caller) {
        emit("RATE_LIMIT_EXCEEDED", toolName, caller, Map.of("severity", "MEDIUM"));
    }

    // ─── Private helpers ──────────────────────────────────────────────────────

    private void emit(String eventType, String toolName, CallerIdentity caller, Map<String, Object> extra) {
        var event = buildBaseEvent(eventType, toolName, caller);
        event.putAll(extra);
        writeEvent(event);
    }

    private LinkedHashMap<String, Object> buildBaseEvent(
            String eventType, String toolName, CallerIdentity caller) {
        var event = new LinkedHashMap<String, Object>();
        event.put("timestamp", Instant.now().toString());
        event.put("eventType", eventType);
        event.put("toolName", toolName);
        if (caller != null) {
            event.put("callerSub", caller.sub());      // Stable user ID — safe to log
            event.put("callerEmail", caller.email());  // Email — safe to log
            event.put("callerUsername", caller.username());
            // NEVER: event.put("callerToken", caller.token()) ← security vulnerability
        }
        return event;
    }

    private void writeEvent(LinkedHashMap<String, Object> event) {
        try {
            AUDIT.info(objectMapper.writeValueAsString(event));
        } catch (Exception e) {
            AUDIT.error("Failed to serialize audit event for type={}: {}",
                    event.get("eventType"), e.getMessage());
        }
    }

    /**
     * Redacts argument values whose keys match known secret field names.
     * This prevents tokens or API keys from appearing in the audit trail.
     */
    private Map<String, Object> sanitizeArguments(Map<String, Object> arguments) {
        if (arguments == null) return Map.of();
        return arguments.entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        e -> isSecretField(e.getKey()) ? "[REDACTED]" : e.getValue(),
                        (a, b) -> a,
                        LinkedHashMap::new));
    }

    private boolean isSecretField(String fieldName) {
        if (fieldName == null) return false;
        String lower = fieldName.toLowerCase();
        return SECRET_FIELD_NAMES.stream().anyMatch(lower::contains);
    }
}
