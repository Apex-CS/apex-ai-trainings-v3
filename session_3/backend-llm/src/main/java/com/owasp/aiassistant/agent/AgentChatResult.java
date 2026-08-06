package com.owasp.aiassistant.agent;

import java.util.List;

public record AgentChatResult(String answer, List<String> warnings, AgentExecutionTrace executionTrace) {

    public AgentChatResult(String answer, List<String> warnings) {
        this(answer, warnings, null);
    }
}
