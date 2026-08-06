package com.owasp.aiassistant.guardrails;

import java.util.List;

public record GuardrailEvaluation(
        boolean blocked,
        String blockReason,
        List<String> softWarnings) {

    public static GuardrailEvaluation allowed(List<String> softWarnings) {
        return new GuardrailEvaluation(false, null, List.copyOf(softWarnings));
    }

    public static GuardrailEvaluation blocked(String reason, List<String> softWarnings) {
        return new GuardrailEvaluation(true, reason, List.copyOf(softWarnings));
    }
}
