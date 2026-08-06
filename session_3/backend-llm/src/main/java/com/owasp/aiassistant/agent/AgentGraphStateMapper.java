package com.owasp.aiassistant.agent;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.owasp.aiassistant.codereview.CodeReviewTraceRedactor;
import org.bsc.langgraph4j.spring.ai.agentexecutor.AgentExecutorEx;

import java.util.LinkedHashMap;
import java.util.Map;

final class AgentGraphStateMapper {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private AgentGraphStateMapper() {
    }

    static Map<String, Object> toTraceState(AgentExecutorEx.State state) {
        Map<String, Object> traceState = new LinkedHashMap<>();
        Object codeToReview = state.data().get(AgentExecutionTrace.CODE_TO_REVIEW_KEY);
        if (codeToReview != null) {
            traceState.put(
                    AgentExecutionTrace.CODE_TO_REVIEW_KEY,
                    codeToReview);
        }
        state.nextAction().ifPresent(value -> traceState.put("next_action", value));
        traceState.put("message_count", state.messages().size());
        return traceState;
    }
}
