package com.owasp.aiassistant.mlflow;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.owasp.aiassistant.agent.AgentExecutionTrace;
import com.owasp.aiassistant.agent.AgentTraceStep;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

final class MlflowSpanJsonBuilder {

    private static final SecureRandom RANDOM = new SecureRandom();

    private static final String ATTR_REQUEST_ID = "mlflow.traceRequestId";
    private static final String ATTR_SPAN_TYPE = "mlflow.spanType";
    private static final String ATTR_INPUTS = "mlflow.spanInputs";
    private static final String ATTR_OUTPUTS = "mlflow.spanOutputs";

    private MlflowSpanJsonBuilder() {
    }

    static String buildTraceDataJson(
            String mlflowTraceId,
            AgentExecutionTrace executionTrace,
            long startTimeNs,
            long endTimeNs,
            ObjectMapper objectMapper) throws JsonProcessingException {
        String otelTraceId = toOtelTraceId(mlflowTraceId);
        String rootSpanId = randomSpanId();

        List<Map<String, Object>> spans = new ArrayList<>();
        spans.add(rootSpan(
                mlflowTraceId,
                otelTraceId,
                rootSpanId,
                startTimeNs,
                endTimeNs,
                executionTrace,
                objectMapper));

        long stepStartNs = startTimeNs;
        long stepDurationNs = Math.max(1, (endTimeNs - startTimeNs) / Math.max(executionTrace.steps().size(), 1));

        for (AgentTraceStep step : executionTrace.steps()) {
            long stepEndNs = Math.min(endTimeNs, stepStartNs + stepDurationNs);
            String stepSpanId = randomSpanId();
            spans.add(stepSpan(
                    mlflowTraceId,
                    otelTraceId,
                    rootSpanId,
                    stepSpanId,
                    stepStartNs,
                    stepEndNs,
                    step,
                    objectMapper));
            stepStartNs = stepEndNs;
        }

        return objectMapper.writeValueAsString(Map.of("spans", spans));
    }

    static Map<String, Object> buildTraceInfo(
            String experimentId,
            String conversationId,
            AgentExecutionTrace executionTrace,
            long startTimeMs,
            long durationMs,
            String status,
            ObjectMapper objectMapper) throws JsonProcessingException {
        Map<String, Object> traceInfo = new LinkedHashMap<>();
        traceInfo.put("trace_location", Map.of(
                "type", "MLFLOW_EXPERIMENT",
                "mlflow_experiment", Map.of("experiment_id", experimentId)));
        traceInfo.put("request_time", toProtoTimestamp(startTimeMs));
        traceInfo.put("execution_duration", toProtoDuration(durationMs));
        traceInfo.put("state", status);
        traceInfo.put("request_preview", objectMapper.writeValueAsString(Map.of(
                "user_message", executionTrace.userMessage(),
                "state", executionTrace.state())));
        traceInfo.put("response_preview", objectMapper.writeValueAsString(Map.of(
                "assistant_answer", executionTrace.assistantAnswer(),
                "state", executionTrace.state())));
        traceInfo.put("trace_metadata", Map.of(
                "mlflow.trace.session", conversationId,
                "conversation_id", conversationId));
        traceInfo.put("tags", Map.of(
                "conversation_id", conversationId,
                "source", "chat-controller"));
        return traceInfo;
    }

    private static Map<String, Object> rootSpan(
            String mlflowTraceId,
            String otelTraceId,
            String spanId,
            long startTimeNs,
            long endTimeNs,
            AgentExecutionTrace executionTrace,
            ObjectMapper objectMapper) throws JsonProcessingException {
        Map<String, Object> inputs = Map.of(
                "user_message", executionTrace.userMessage(),
                "state", executionTrace.state());
        Map<String, Object> outputs = Map.of(
                "assistant_answer", executionTrace.assistantAnswer(),
                "state", executionTrace.state(),
                "steps", executionTrace.steps().size());

        return span(
                "chat_agent",
                "AGENT",
                mlflowTraceId,
                otelTraceId,
                spanId,
                null,
                startTimeNs,
                endTimeNs,
                inputs,
                outputs,
                objectMapper);
    }

    private static Map<String, Object> stepSpan(
            String mlflowTraceId,
            String otelTraceId,
            String parentSpanId,
            String spanId,
            long startTimeNs,
            long endTimeNs,
            AgentTraceStep step,
            ObjectMapper objectMapper) throws JsonProcessingException {
        String spanType = spanTypeFor(step.node());
        Map<String, Object> inputs = Map.of(
                "node", step.node(),
                "messages", step.messages(),
                "state", step.state());
        Map<String, Object> outputs = Map.of(
                "tool_calls", step.toolCalls(),
                "state", step.state());

        return span(
                step.node(),
                spanType,
                mlflowTraceId,
                otelTraceId,
                spanId,
                parentSpanId,
                startTimeNs,
                endTimeNs,
                inputs,
                outputs,
                objectMapper);
    }

    private static Map<String, Object> span(
            String name,
            String spanType,
            String mlflowTraceId,
            String otelTraceId,
            String spanId,
            String parentSpanId,
            long startTimeNs,
            long endTimeNs,
            Map<String, Object> inputs,
            Map<String, Object> outputs,
            ObjectMapper objectMapper) throws JsonProcessingException {
        Map<String, Object> span = new LinkedHashMap<>();
        span.put("name", name);
        span.put("context", Map.of(
                "span_id", spanId,
                "trace_id", otelTraceId));
        span.put("parent_id", parentSpanId);
        span.put("start_time", startTimeNs);
        span.put("end_time", endTimeNs);
        span.put("status_code", "OK");
        span.put("status_message", "");
        span.put("attributes", Map.of(
                ATTR_REQUEST_ID, mlflowTraceId,
                ATTR_SPAN_TYPE, spanType,
                ATTR_INPUTS, objectMapper.writeValueAsString(inputs),
                ATTR_OUTPUTS, objectMapper.writeValueAsString(outputs)));
        span.put("events", List.of());
        return span;
    }

    private static String spanTypeFor(String node) {
        if ("model".equals(node)) {
            return "LLM";
        }
        if ("action_dispatcher".equals(node) || "stop".equals(node)) {
            return "CHAIN";
        }
        return "TOOL";
    }

    private static String randomSpanId() {
        return randomHex(16);
    }

    private static String toOtelTraceId(String mlflowTraceId) {
        String normalized = mlflowTraceId.startsWith("tr-") ? mlflowTraceId.substring(3) : mlflowTraceId;
        if (normalized.length() >= 32) {
            return normalized.substring(0, 32);
        }
        return String.format("%32s", normalized).replace(' ', '0');
    }

    private static String randomHex(int length) {
        byte[] bytes = new byte[length / 2];
        RANDOM.nextBytes(bytes);
        StringBuilder builder = new StringBuilder(length);
        for (byte value : bytes) {
            builder.append(String.format("%02x", value));
        }
        return builder.substring(0, length);
    }

    private static String toProtoTimestamp(long epochMs) {
        return java.time.Instant.ofEpochMilli(epochMs).toString();
    }

    static String toProtoDuration(long durationMs) {
        if (durationMs <= 0) {
            return "0s";
        }
        if (durationMs % 1000 == 0) {
            return (durationMs / 1000) + "s";
        }
        return String.format(Locale.US, "%.3fs", durationMs / 1000.0);
    }
}
