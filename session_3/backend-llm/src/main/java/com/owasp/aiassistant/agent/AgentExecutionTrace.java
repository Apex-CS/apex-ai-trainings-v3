package com.owasp.aiassistant.agent;

import java.util.List;
import java.util.Map;

public record AgentExecutionTrace(
        String userMessage,
        String assistantAnswer,
        Map<String, Object> state,
        List<AgentTraceStep> steps) {

    public static final String CODE_TO_REVIEW_KEY = "code_to_review";
    public static final String DEFAULT_CODE_TO_REVIEW = "print('hello')";
}
