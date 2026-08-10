package com.owasp.aiassistant.policy;

public record PolicyViolation(PolicyViolationType type, String reason, String source) {
}
