package com.owasp.aiassistant.mlflow;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.owasp.aiassistant.agent.AgentExecutionTrace;
import com.owasp.aiassistant.agent.AgentTraceStep;
import com.owasp.aiassistant.corporate.enums.DemoUser;
import com.owasp.aiassistant.policy.PolicyViolationStateKeys;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertTrue;

class MlflowSpanJsonBuilderTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void buildTraceDataJson_includesAgentStateAndToolSteps() throws Exception {
        AgentExecutionTrace executionTrace = new AgentExecutionTrace(
                "hello",
                "world",
                Map.of(AgentExecutionTrace.CODE_TO_REVIEW_KEY, AgentExecutionTrace.DEFAULT_CODE_TO_REVIEW),
                List.of(new AgentTraceStep(
                        "searchWeb",
                        1L,
                        List.of(Map.of("role", "assistant", "content", "searching")),
                        List.of(Map.of("name", "searchWeb", "arguments", "{\"query\":\"weather\"}")),
                        Map.of(AgentExecutionTrace.CODE_TO_REVIEW_KEY, AgentExecutionTrace.DEFAULT_CODE_TO_REVIEW))));

        String traceDataJson = MlflowSpanJsonBuilder.buildTraceDataJson(
                "tr-test123456789012345678901234567890",
                executionTrace,
                1_000_000L,
                2_000_000L,
                objectMapper);

        assertTrue(traceDataJson.contains("chat_agent"));
        assertTrue(traceDataJson.contains("searchWeb"));
        assertTrue(traceDataJson.contains("code_to_review"));
        assertTrue(traceDataJson.contains("print('hello')"));
        assertTrue(traceDataJson.contains("mlflow.spanInputs"));
        assertTrue(traceDataJson.contains("mlflow.spanOutputs"));
    }

    @Test
    void buildTraceInfo_usesProtobufDurationFormat() throws Exception {
        Map<String, Object> traceInfo = MlflowSpanJsonBuilder.buildTraceInfo(
                "1935323079482537",
                "conversation-1",
                new AgentExecutionTrace(
                        "hello",
                        "world",
                        Map.of(AgentExecutionTrace.CODE_TO_REVIEW_KEY, AgentExecutionTrace.DEFAULT_CODE_TO_REVIEW),
                        List.of()),
                1_700_000_000_000L,
                1234L,
                "OK",
                DemoUser.BART_PEREZ,
                objectMapper);

        org.junit.jupiter.api.Assertions.assertEquals("1.234s", traceInfo.get("execution_duration"));
        org.junit.jupiter.api.Assertions.assertEquals("5s", MlflowSpanJsonBuilder.toProtoDuration(5000L));
    }

    @Test
    void buildTraceInfo_includesPolicyViolationTags() throws Exception {
        Map<String, Object> state = new java.util.LinkedHashMap<>();
        state.put(PolicyViolationStateKeys.SOFT_COUNT, 2);
        state.put(PolicyViolationStateKeys.HARD_COUNT, 1);
        state.put(PolicyViolationStateKeys.VIOLATIONS, List.of(
                Map.of("type", "HARD", "reason", "User attempted to restart app without permissions")));

        Map<String, Object> traceInfo = MlflowSpanJsonBuilder.buildTraceInfo(
                "1935323079482537",
                "conversation-1",
                new AgentExecutionTrace("hello", "world", state, List.of()),
                1_700_000_000_000L,
                1234L,
                "OK",
                DemoUser.SUTANO_DOE,
                objectMapper);

        @SuppressWarnings("unchecked")
        Map<String, String> tags = (Map<String, String>) traceInfo.get("tags");
        org.junit.jupiter.api.Assertions.assertEquals("2", tags.get(PolicyViolationStateKeys.MLFLOW_TAG_SOFT));
        org.junit.jupiter.api.Assertions.assertEquals("1", tags.get(PolicyViolationStateKeys.MLFLOW_TAG_HARD));
        org.junit.jupiter.api.Assertions.assertEquals("Sutano Doe", tags.get(MlflowDemoUserTags.TAG_USER_NAME));
        org.junit.jupiter.api.Assertions.assertEquals(
                "sales-admin,financial-user,it-user,marketing-user",
                tags.get(MlflowDemoUserTags.TAG_USER_ROLES));
    }
}
