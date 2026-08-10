package com.owasp.aiassistant.mlflow;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.owasp.aiassistant.agent.AgentChatResult;
import com.owasp.aiassistant.config.MlflowProperties;
import com.owasp.aiassistant.corporate.config.CorporateApiProperties;
import com.owasp.aiassistant.corporate.enums.DemoUser;
import com.owasp.aiassistant.policy.PolicyViolationStateKeys;
import org.mlflow.tracking.MlflowClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.LinkedHashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Service
@ConditionalOnBean(MlflowClient.class)
public class MlflowChatTracingService {

    private static final Logger log = LoggerFactory.getLogger(MlflowChatTracingService.class);
    private static final int MAX_LOGGED_TEXT_LENGTH = 5000;

    private final MlflowClient mlflowClient;
    private final MlflowTraceApiClient traceApiClient;
    private final MlflowProperties properties;
    private final CorporateApiProperties corporateApiProperties;
    private final ObjectMapper objectMapper;
    private final ConcurrentHashMap<String, ConversationState> conversationStates = new ConcurrentHashMap<>();

    private volatile String experimentId;

    public MlflowChatTracingService(
            MlflowClient mlflowClient,
            MlflowTraceApiClient traceApiClient,
            MlflowProperties properties,
            CorporateApiProperties corporateApiProperties,
            ObjectMapper objectMapper) {
        this.mlflowClient = mlflowClient;
        this.traceApiClient = traceApiClient;
        this.properties = properties;
        this.corporateApiProperties = corporateApiProperties;
        this.objectMapper = objectMapper;
    }

    public void recordChatTurn(
            String conversationId,
            String userMessage,
            AgentChatResult result,
            long durationMs,
            DemoUser demoUser) {
        if (!properties.isEnabled()) {
            return;
        }

        long endTimeMs = System.currentTimeMillis();
        long startTimeMs = endTimeMs - durationMs;
        String traceRequestId = null;
        DemoUser effectiveDemoUser = MlflowDemoUserTags.resolveDemoUser(demoUser, corporateApiProperties);

        try {
            log.debug("Recording MLflow chat turn for conversation {} (duration={} ms)", conversationId, durationMs);

            if (result.executionTrace() != null) {
                traceRequestId = traceApiClient.logCompletedTrace(
                        experimentId(),
                        conversationId,
                        result.executionTrace(),
                        startTimeMs,
                        durationMs,
                        "OK",
                        effectiveDemoUser);
            } else {
                traceRequestId = recordLegacyChatTurn(
                        conversationId,
                        userMessage,
                        result,
                        durationMs,
                        startTimeMs,
                        endTimeMs,
                        effectiveDemoUser);
            }

            ConversationState state = getOrCreateConversationState(conversationId);
            int turn = state.nextTurn();
            logConversationRun(state.runId(), turn, userMessage, result, durationMs, effectiveDemoUser);

            log.debug(
                    "Recorded MLflow chat turn {} for conversation {} (trace={})",
                    turn,
                    conversationId,
                    traceRequestId);
        } catch (Exception e) {
            log.warn("Failed to record MLflow chat trace for conversation {}: {}", conversationId, e.getMessage());
            endTraceOnError(traceRequestId, conversationId, startTimeMs, endTimeMs, e);
        }
    }

    private String recordLegacyChatTurn(
            String conversationId,
            String userMessage,
            AgentChatResult result,
            long durationMs,
            long startTimeMs,
            long endTimeMs,
            DemoUser demoUser) {
        Map<String, String> userTags = MlflowDemoUserTags.forUser(demoUser);
        Map<String, String> startTags = new LinkedHashMap<>();
        startTags.put("conversation_id", conversationId);
        startTags.put("source", "chat-controller");
        startTags.putAll(userTags);

        String traceRequestId = traceApiClient.startTrace(
                experimentId(),
                startTimeMs,
                Map.of(
                        "session_id", conversationId,
                        "mlflow.trace.session", conversationId,
                        "user_message", truncate(userMessage)),
                startTags);

        Map<String, String> endTags = new LinkedHashMap<>();
        endTags.put("conversation_id", conversationId);
        endTags.put("latency_ms", String.valueOf(durationMs));
        endTags.putAll(userTags);

        traceApiClient.endTrace(
                traceRequestId,
                endTimeMs,
                "OK",
                Map.of(
                        "session_id", conversationId,
                        "mlflow.trace.session", conversationId,
                        "assistant_response", truncate(result.answer()),
                        "warnings", serializeWarnings(result.warnings())),
                endTags);
        return traceRequestId;
    }

    public void recordChatError(
            String conversationId,
            String userMessage,
            long durationMs,
            Exception error,
            DemoUser demoUser) {
        if (!properties.isEnabled()) {
            return;
        }

        long endTimeMs = System.currentTimeMillis();
        long startTimeMs = endTimeMs - durationMs;
        String traceRequestId = null;
        DemoUser effectiveDemoUser = MlflowDemoUserTags.resolveDemoUser(demoUser, corporateApiProperties);

        try {
            log.debug(
                    "Recording MLflow chat error for conversation {} (duration={} ms): {}",
                    conversationId,
                    durationMs,
                    error.getClass().getSimpleName());

            Map<String, String> userTags = MlflowDemoUserTags.forUser(effectiveDemoUser);
            Map<String, String> startTags = new LinkedHashMap<>();
            startTags.put("conversation_id", conversationId);
            startTags.put("source", "chat-controller");
            startTags.putAll(userTags);

            traceRequestId = traceApiClient.startTrace(
                    experimentId(),
                    startTimeMs,
                    Map.of(
                            "session_id", conversationId,
                            "mlflow.trace.session", conversationId,
                            "user_message", truncate(userMessage)),
                    startTags);

            ConversationState state = getOrCreateConversationState(conversationId);
            int turn = state.nextTurn();
            String errorMessage = truncate(error.getMessage() != null ? error.getMessage() : error.getClass().getSimpleName());

            mlflowClient.logParam(state.runId(), "turn." + turn + ".user_message", truncate(userMessage));
            mlflowClient.logParam(state.runId(), "turn." + turn + ".status", "ERROR");
            mlflowClient.logParam(state.runId(), "turn." + turn + ".error", errorMessage);
            mlflowClient.logMetric(state.runId(), "turn." + turn + ".latency_ms", durationMs);
            mlflowClient.logMetric(state.runId(), "message_count", turn);
            logDemoUserTags(state.runId(), effectiveDemoUser);

            Map<String, String> endTags = new LinkedHashMap<>();
            endTags.put("conversation_id", conversationId);
            endTags.put("turn", String.valueOf(turn));
            endTags.put("latency_ms", String.valueOf(durationMs));
            endTags.putAll(userTags);

            traceApiClient.endTrace(
                    traceRequestId,
                    endTimeMs,
                    "ERROR",
                    Map.of(
                            "session_id", conversationId,
                            "mlflow.trace.session", conversationId,
                            "error", errorMessage),
                    endTags);
        } catch (Exception e) {
            log.warn("Failed to record MLflow chat error for conversation {}: {}", conversationId, e.getMessage());
            endTraceOnError(traceRequestId, conversationId, startTimeMs, endTimeMs, e);
        }
    }

    private void logConversationRun(
            String runId,
            int turn,
            String userMessage,
            AgentChatResult result,
            long durationMs,
            DemoUser demoUser) {
        mlflowClient.logParam(runId, "turn." + turn + ".user_message", truncate(userMessage));
        mlflowClient.logParam(runId, "turn." + turn + ".assistant_response", truncate(result.answer()));
        mlflowClient.logParam(runId, "turn." + turn + ".status", "OK");

        if (!result.warnings().isEmpty()) {
            mlflowClient.logParam(runId, "turn." + turn + ".warnings", serializeWarnings(result.warnings()));
        }

        if (result.executionTrace() != null && !result.executionTrace().state().isEmpty()) {
            try {
                mlflowClient.logParam(
                        runId,
                        "turn." + turn + ".state",
                        truncate(objectMapper.writeValueAsString(result.executionTrace().state())));
            } catch (JsonProcessingException e) {
                mlflowClient.logParam(runId, "turn." + turn + ".state", result.executionTrace().state().toString());
            }
            logPolicyViolations(runId, turn, result);
        }

        mlflowClient.logMetric(runId, "turn." + turn + ".latency_ms", durationMs);
        mlflowClient.logMetric(runId, "message_count", turn);
        logDemoUserTags(runId, demoUser);
    }

    private void logDemoUserTags(String runId, DemoUser demoUser) {
        MlflowDemoUserTags.forUser(demoUser).forEach((key, value) -> mlflowClient.setTag(runId, key, value));
    }

    private void logPolicyViolations(String runId, int turn, AgentChatResult result) {
        if (result.executionTrace() == null) {
            return;
        }

        int softCount = PolicyViolationStateKeys.softCount(result.executionTrace().state());
        int hardCount = PolicyViolationStateKeys.hardCount(result.executionTrace().state());

        mlflowClient.logMetric(runId, "turn." + turn + "." + PolicyViolationStateKeys.MLFLOW_TAG_SOFT, softCount);
        mlflowClient.logMetric(runId, "turn." + turn + "." + PolicyViolationStateKeys.MLFLOW_TAG_HARD, hardCount);
        mlflowClient.setTag(runId, PolicyViolationStateKeys.MLFLOW_TAG_SOFT, String.valueOf(softCount));
        mlflowClient.setTag(runId, PolicyViolationStateKeys.MLFLOW_TAG_HARD, String.valueOf(hardCount));

        if (!PolicyViolationStateKeys.violations(result.executionTrace().state()).isEmpty()) {
            try {
                mlflowClient.logParam(
                        runId,
                        "turn." + turn + ".policy_violations",
                        truncate(objectMapper.writeValueAsString(
                                PolicyViolationStateKeys.violations(result.executionTrace().state()))));
            } catch (JsonProcessingException e) {
                mlflowClient.logParam(
                        runId,
                        "turn." + turn + ".policy_violations",
                        PolicyViolationStateKeys.violations(result.executionTrace().state()).toString());
            }
        }
    }

    private ConversationState getOrCreateConversationState(String conversationId) {
        ConversationState cached = conversationStates.get(conversationId);
        if (cached != null) {
            return cached;
        }

        return conversationStates.computeIfAbsent(conversationId, this::loadOrCreateConversationState);
    }

    private ConversationState loadOrCreateConversationState(String conversationId) {
        String filter = "tags.conversation_id = '" + escapeFilterValue(conversationId) + "'";
        var runsPage = mlflowClient.searchRuns(
                List.of(experimentId()),
                filter,
                org.mlflow.api.proto.Service.ViewType.ACTIVE_ONLY,
                1);

        if (!runsPage.getItems().isEmpty()) {
            String runId = runsPage.getItems().getFirst().getInfo().getRunId();
            int existingTurns = readMessageCount(runId);
            log.debug("Reusing MLflow run {} for conversation {} (existing turns={})", runId, conversationId, existingTurns);
            return new ConversationState(runId, new AtomicInteger(existingTurns));
        }

        long startTime = System.currentTimeMillis();
        org.mlflow.api.proto.Service.RunTag conversationTag = org.mlflow.api.proto.Service.RunTag.newBuilder()
                .setKey("conversation_id")
                .setValue(conversationId)
                .build();
        org.mlflow.api.proto.Service.RunTag sourceTag = org.mlflow.api.proto.Service.RunTag.newBuilder()
                .setKey("source")
                .setValue("chat-controller")
                .build();

        org.mlflow.api.proto.Service.CreateRun createRunRequest = org.mlflow.api.proto.Service.CreateRun.newBuilder()
                .setExperimentId(experimentId())
                .setStartTime(startTime)
                .setRunName(conversationId)
                .addTags(conversationTag)
                .addTags(sourceTag)
                .build();

        String runId = mlflowClient.createRun(createRunRequest).getRunId();
        log.debug("Created MLflow run {} for conversation {}", runId, conversationId);

        return new ConversationState(runId, new AtomicInteger(0));
    }

    private int readMessageCount(String runId) {
        List<org.mlflow.api.proto.Service.Metric> metrics = mlflowClient.getMetricHistory(runId, "message_count");
        if (metrics.isEmpty()) {
            return 0;
        }
        return (int) metrics.getLast().getValue();
    }

    private String experimentId() {
        if (experimentId != null) {
            return experimentId;
        }

        synchronized (this) {
            if (experimentId != null) {
                return experimentId;
            }

            Optional<org.mlflow.api.proto.Service.Experiment> experiment =
                    mlflowClient.getExperimentByName(properties.getExperimentName());
            if (experiment.isPresent()) {
                experimentId = experiment.get().getExperimentId();
                log.debug("Resolved MLflow experiment '{}' to id {}", properties.getExperimentName(), experimentId);
            } else if (properties.isAutoCreateExperiment()) {
                experimentId = mlflowClient.createExperiment(properties.getExperimentName());
                log.debug("Created MLflow experiment '{}' with id {}", properties.getExperimentName(), experimentId);
            } else {
                throw new IllegalStateException(
                        "MLflow experiment '" + properties.getExperimentName() + "' was not found");
            }
            return experimentId;
        }
    }

    private void endTraceOnError(
            String traceRequestId,
            String conversationId,
            long startTimeMs,
            long endTimeMs,
            Exception error) {
        if (traceRequestId == null) {
            return;
        }

        try {
            traceApiClient.endTrace(
                    traceRequestId,
                    endTimeMs,
                    "ERROR",
                    Map.of(
                            "session_id", conversationId,
                            "mlflow.trace.session", conversationId,
                            "error", truncate(error.getMessage())),
                    Map.of("conversation_id", conversationId));
        } catch (Exception endError) {
            log.debug("Failed to end MLflow trace {} after error: {}", traceRequestId, endError.getMessage());
        }
    }

    private String serializeWarnings(List<String> warnings) {
        try {
            return truncate(objectMapper.writeValueAsString(warnings));
        } catch (JsonProcessingException e) {
            return truncate(String.join("; ", warnings));
        }
    }

    private static String truncate(String value) {
        if (value == null) {
            return "";
        }
        if (value.length() <= MAX_LOGGED_TEXT_LENGTH) {
            return value;
        }
        return value.substring(0, MAX_LOGGED_TEXT_LENGTH) + "...";
    }

    private static String escapeFilterValue(String value) {
        return value.replace("'", "\\'");
    }

    private static final class ConversationState {
        private final String runId;
        private final AtomicInteger turnCount;

        private ConversationState(String runId, AtomicInteger turnCount) {
            this.runId = runId;
            this.turnCount = turnCount;
        }

        private String runId() {
            return runId;
        }

        private int nextTurn() {
            return turnCount.incrementAndGet();
        }
    }
}
