package com.owasp.aiassistant.guardrails;

public record GuardrailPolicy(
        String id,
        String name,
        GuardrailType type,
        GuardrailEnforcement enforcement,
        String content,
        String source) {
}
