package com.owasp.aiassistant.mlflow;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.owasp.aiassistant.agent.AgentExecutionTrace;
import com.owasp.aiassistant.agent.AgentTraceStep;
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
                objectMapper);

        org.junit.jupiter.api.Assertions.assertEquals("1.234s", traceInfo.get("execution_duration"));
        org.junit.jupiter.api.Assertions.assertEquals("5s", MlflowSpanJsonBuilder.toProtoDuration(5000L));
    }
}
