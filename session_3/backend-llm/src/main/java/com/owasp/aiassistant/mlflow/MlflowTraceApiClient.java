package com.owasp.aiassistant.mlflow;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.owasp.aiassistant.agent.AgentExecutionTrace;
import com.owasp.aiassistant.config.DatabricksProperties;
import com.owasp.aiassistant.config.MlflowProperties;
import com.owasp.aiassistant.corporate.enums.DemoUser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.util.Map;

@Component
@ConditionalOnProperty(name = "app.mlflow.enabled", havingValue = "true", matchIfMissing = true)
public class MlflowTraceApiClient {

    private static final Logger log = LoggerFactory.getLogger(MlflowTraceApiClient.class);

    private final MlflowProperties properties;
    private final WebClient webClient;
    private final ObjectMapper objectMapper;

    public MlflowTraceApiClient(
            MlflowProperties properties,
            DatabricksProperties databricksProperties,
            WebClient.Builder webClientBuilder,
            ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        WebClient.Builder builder = webClientBuilder
                .baseUrl(properties.resolveApiBaseUrl(databricksProperties.getHost()));
        if (properties.isDatabricks()) {
            builder = builder.defaultHeader("Authorization", "Bearer " + requireToken(databricksProperties));
        }
        this.webClient = builder.build();
        log.debug(
                "MLflow trace client configured for {} backend at {}",
                properties.isDatabricks() ? "Databricks" : "local",
                properties.resolveApiBaseUrl(databricksProperties.getHost()));
    }

    public String logCompletedTrace(
            String experimentId,
            String conversationId,
            AgentExecutionTrace executionTrace,
            long startTimeMs,
            long durationMs,
            String status,
            DemoUser demoUser) throws JsonProcessingException {
        if (properties.isDatabricks()) {
            return logCompletedTraceV3(
                    experimentId, conversationId, executionTrace, startTimeMs, durationMs, status, demoUser);
        }
        return logCompletedTraceV2(
                experimentId, conversationId, executionTrace, startTimeMs, durationMs, status, demoUser);
    }

    public String startTrace(
            String experimentId,
            long startTimeMs,
            Map<String, String> requestMetadata,
            Map<String, String> tags) {
        ObjectNode body = objectMapper.createObjectNode();
        body.put("experiment_id", experimentId);
        body.put("timestamp_ms", startTimeMs);
        body.set("request_metadata", toKeyValueArray(requestMetadata));
        body.set("tags", toKeyValueArray(tags));

        log.debug("Starting MLflow trace for experiment {} at {} ms", experimentId, startTimeMs);

        JsonNode response = webClient.post()
                .uri("/api/2.0/mlflow/traces")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .retrieve()
                .bodyToMono(JsonNode.class)
                .block();

        if (response == null || !response.hasNonNull("trace_info")) {
            throw new IllegalStateException("MLflow startTrace response missing trace_info");
        }

        JsonNode traceInfo = response.get("trace_info");
        if (!traceInfo.hasNonNull("request_id")) {
            throw new IllegalStateException("MLflow startTrace response missing request_id");
        }

        String requestId = traceInfo.get("request_id").asText();
        log.debug("Started MLflow trace {} for experiment {}", requestId, experimentId);
        return requestId;
    }

    public void endTrace(
            String requestId,
            long endTimeMs,
            String status,
            Map<String, String> requestMetadata,
            Map<String, String> tags) {
        ObjectNode body = objectMapper.createObjectNode();
        body.put("request_id", requestId);
        body.put("timestamp_ms", endTimeMs);
        body.put("status", status);
        body.set("request_metadata", toKeyValueArray(requestMetadata));
        body.set("tags", toKeyValueArray(tags));

        log.debug("Ending MLflow trace {} with status {} at {} ms", requestId, status, endTimeMs);

        webClient.patch()
                .uri("/api/2.0/mlflow/traces/{requestId}", requestId)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .retrieve()
                .toBodilessEntity()
                .block();
    }

    private String logCompletedTraceV3(
            String experimentId,
            String conversationId,
            AgentExecutionTrace executionTrace,
            long startTimeMs,
            long durationMs,
            String status,
            DemoUser demoUser) throws JsonProcessingException {
        Map<String, Object> traceInfo = MlflowSpanJsonBuilder.buildTraceInfo(
                experimentId,
                conversationId,
                executionTrace,
                startTimeMs,
                durationMs,
                status,
                demoUser,
                objectMapper);

        ObjectNode body = objectMapper.createObjectNode();
        body.set("trace", objectMapper.createObjectNode().set("trace_info", objectMapper.valueToTree(traceInfo)));

        log.debug("Posting MLflow v3 trace payload: {}", body);

        JsonNode response;
        try {
            response = webClient.post()
                    .uri("/api/3.0/mlflow/traces")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(body)
                    .retrieve()
                    .bodyToMono(JsonNode.class)
                    .block();
        } catch (WebClientResponseException e) {
            throw new IllegalStateException(
                    "MLflow v3 trace creation failed: " + e.getStatusCode().value() + " "
                            + e.getStatusText() + " body=" + e.getResponseBodyAsString(),
                    e);
        }

        String traceId = extractTraceId(response);
        long startTimeNs = startTimeMs * 1_000_000L;
        long endTimeNs = (startTimeMs + durationMs) * 1_000_000L;
        String traceDataJson = MlflowSpanJsonBuilder.buildTraceDataJson(
                traceId, executionTrace, startTimeNs, endTimeNs, objectMapper);
        uploadTraceData(traceId, traceDataJson);
        log.debug("Logged MLflow v3 trace {} with {} spans", traceId, executionTrace.steps().size() + 1);
        return traceId;
    }

    private String logCompletedTraceV2(
            String experimentId,
            String conversationId,
            AgentExecutionTrace executionTrace,
            long startTimeMs,
            long durationMs,
            String status,
            DemoUser demoUser) throws JsonProcessingException {
        Map<String, String> userTags = MlflowDemoUserTags.forUser(demoUser);
        Map<String, String> startTags = new java.util.LinkedHashMap<>();
        startTags.put("conversation_id", conversationId);
        startTags.put("source", "chat-controller");
        startTags.putAll(userTags);

        String traceRequestId = startTrace(
                experimentId,
                startTimeMs,
                Map.of(
                        "session_id", conversationId,
                        "mlflow.trace.session", conversationId,
                        "user_message", truncate(executionTrace.userMessage()),
                        "state", objectMapper.writeValueAsString(executionTrace.state()),
                        "steps", String.valueOf(executionTrace.steps().size())),
                startTags);

        Map<String, String> endTags = new java.util.LinkedHashMap<>();
        endTags.put("conversation_id", conversationId);
        endTags.put("latency_ms", String.valueOf(durationMs));
        endTags.putAll(userTags);

        endTrace(
                traceRequestId,
                startTimeMs + durationMs,
                status,
                Map.of(
                        "session_id", conversationId,
                        "mlflow.trace.session", conversationId,
                        "assistant_response", truncate(executionTrace.assistantAnswer()),
                        "state", objectMapper.writeValueAsString(executionTrace.state()),
                        "messages", objectMapper.writeValueAsString(executionTrace.steps())),
                endTags);

        try {
            long startTimeNs = startTimeMs * 1_000_000L;
            long endTimeNs = (startTimeMs + durationMs) * 1_000_000L;
            String traceDataJson = MlflowSpanJsonBuilder.buildTraceDataJson(
                    traceRequestId, executionTrace, startTimeNs, endTimeNs, objectMapper);
            uploadTraceData(traceRequestId, traceDataJson);
        } catch (Exception uploadError) {
            log.debug("Local MLflow trace span upload skipped: {}", uploadError.getMessage());
        }

        return traceRequestId;
    }

    private void uploadTraceData(String traceId, String traceDataJson) {
        JsonNode credentialsResponse = webClient.get()
                .uri("/api/3.0/mlflow/traces/{traceId}/credentials-for-data-upload", traceId)
                .retrieve()
                .bodyToMono(JsonNode.class)
                .block();

        if (credentialsResponse == null || !credentialsResponse.hasNonNull("credential_info")) {
            throw new IllegalStateException("MLflow trace upload credentials response missing credential_info");
        }

        JsonNode credentialInfo = credentialsResponse.get("credential_info");
        if (!credentialInfo.hasNonNull("signed_uri")) {
            throw new IllegalStateException("MLflow trace upload credentials response missing signed_uri");
        }

        WebClient uploadClient = WebClient.builder().build();
        WebClient.RequestBodySpec uploadRequest = uploadClient.put()
                .uri(credentialInfo.get("signed_uri").asText())
                .contentType(MediaType.APPLICATION_JSON);

        if (credentialInfo.has("headers") && credentialInfo.get("headers").isArray()) {
            HttpHeaders headers = new HttpHeaders();
            for (JsonNode header : credentialInfo.get("headers")) {
                if (header.hasNonNull("name") && header.hasNonNull("value")) {
                    headers.add(header.get("name").asText(), header.get("value").asText());
                }
            }
            uploadRequest = uploadRequest.headers(httpHeaders -> httpHeaders.addAll(headers));
        }

        uploadRequest.bodyValue(traceDataJson).retrieve().toBodilessEntity().block();
    }

    private String extractTraceId(JsonNode response) {
        if (response == null || !response.hasNonNull("trace")) {
            throw new IllegalStateException("MLflow v3 trace response missing trace");
        }
        JsonNode traceInfo = response.get("trace").get("trace_info");
        if (traceInfo == null) {
            throw new IllegalStateException("MLflow v3 trace response missing trace_info");
        }
        if (traceInfo.hasNonNull("trace_id")) {
            return traceInfo.get("trace_id").asText();
        }
        if (traceInfo.hasNonNull("request_id")) {
            return traceInfo.get("request_id").asText();
        }
        throw new IllegalStateException("MLflow v3 trace response missing trace_id");
    }

    private static String truncate(String value) {
        if (value == null) {
            return "";
        }
        if (value.length() <= 5000) {
            return value;
        }
        return value.substring(0, 5000) + "...";
    }

    private ArrayNode toKeyValueArray(Map<String, String> values) {
        ArrayNode array = objectMapper.createArrayNode();
        values.forEach((key, value) -> {
            ObjectNode entry = objectMapper.createObjectNode();
            entry.put("key", key);
            entry.put("value", value);
            array.add(entry);
        });
        return array;
    }

    private static String requireToken(DatabricksProperties databricksProperties) {
        String token = databricksProperties.getToken();
        if (token == null || token.isBlank()) {
            throw new IllegalStateException(
                    "app.databricks.token (DATABRICKS_TOKEN) is required when MLFLOW_TRACKING_URI=databricks");
        }
        return token;
    }
}
