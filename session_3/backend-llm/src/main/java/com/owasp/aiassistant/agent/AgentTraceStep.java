package com.owasp.aiassistant.agent;

import java.util.List;
import java.util.Map;

public record AgentTraceStep(
        String node,
        long timestampMs,
        List<Map<String, Object>> messages,
        List<Map<String, Object>> toolCalls,
        Map<String, Object> state) {
}
