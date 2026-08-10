package com.owasp.aiassistant.policy;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public record PolicyViolationSummary(int softCount, int hardCount, List<PolicyViolation> violations) {

    public Map<String, Object> toStateEntries() {
        Map<String, Object> state = new LinkedHashMap<>();
        state.put(PolicyViolationStateKeys.SOFT_COUNT, softCount);
        state.put(PolicyViolationStateKeys.HARD_COUNT, hardCount);
        state.put(PolicyViolationStateKeys.VIOLATIONS, violations.stream()
                .map(PolicyViolationStateKeys::toMap)
                .toList());
        return state;
    }
}
