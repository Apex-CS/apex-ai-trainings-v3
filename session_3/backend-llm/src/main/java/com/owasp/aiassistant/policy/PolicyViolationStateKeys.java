package com.owasp.aiassistant.policy;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class PolicyViolationStateKeys {

    public static final String SOFT_COUNT = "soft_policy_violations";
    public static final String HARD_COUNT = "hard_policy_violations";
    public static final String VIOLATIONS = "policy_violations";

    public static final String MLFLOW_TAG_SOFT = "soft policy violations";
    public static final String MLFLOW_TAG_HARD = "hard policy violations";

    private PolicyViolationStateKeys() {
    }

    public static Map<String, Object> toMap(PolicyViolation violation) {
        Map<String, Object> entry = new LinkedHashMap<>();
        entry.put("type", violation.type().name());
        entry.put("reason", violation.reason());
        if (violation.source() != null && !violation.source().isBlank()) {
            entry.put("source", violation.source());
        }
        return entry;
    }

    public static int softCount(Map<String, Object> state) {
        return readCount(state, SOFT_COUNT);
    }

    public static int hardCount(Map<String, Object> state) {
        return readCount(state, HARD_COUNT);
    }

    @SuppressWarnings("unchecked")
    public static List<Map<String, Object>> violations(Map<String, Object> state) {
        Object value = state.get(VIOLATIONS);
        if (value instanceof List<?> list) {
            return list.stream()
                    .filter(Map.class::isInstance)
                    .map(item -> (Map<String, Object>) item)
                    .toList();
        }
        return List.of();
    }

    private static int readCount(Map<String, Object> state, String key) {
        if (state == null) {
            return 0;
        }
        Object value = state.get(key);
        if (value instanceof Number number) {
            return number.intValue();
        }
        return 0;
    }
}
