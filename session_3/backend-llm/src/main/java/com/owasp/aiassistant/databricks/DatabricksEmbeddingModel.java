package com.owasp.aiassistant.databricks;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.owasp.aiassistant.config.DatabricksProperties;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.Embedding;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.EmbeddingRequest;
import org.springframework.ai.embedding.EmbeddingResponse;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.util.ArrayList;
import java.util.List;

public class DatabricksEmbeddingModel implements EmbeddingModel {

    private final WebClient webClient;
    private final DatabricksProperties properties;
    private final ObjectMapper objectMapper;
    private final String invocationUrl;
    private volatile Integer dimensions;

    public DatabricksEmbeddingModel(
            WebClient.Builder webClientBuilder,
            DatabricksProperties properties,
            ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.invocationUrl = properties.resolveEmbeddingInvocationUrl();
        this.webClient = webClientBuilder
                .defaultHeader("Authorization", "Bearer " + properties.getToken())
                .build();
    }

    @Override
    public EmbeddingResponse call(EmbeddingRequest request) {
        List<String> inputs = request.getInstructions();
        List<Embedding> embeddings = new ArrayList<>();

        for (int index = 0; index < inputs.size(); index++) {
            float[] vector = embedText(inputs.get(index));
            embeddings.add(new Embedding(vector, index));
        }

        return new EmbeddingResponse(embeddings);
    }

    @Override
    public float[] embed(Document document) {
        return embed(document.getText());
    }

    @Override
    public int dimensions() {
        if (dimensions == null) {
            float[] probe = embed("dimension probe");
            dimensions = probe.length;
        }
        return dimensions;
    }

    private float[] embedText(String text) {
        ObjectNode body = objectMapper.createObjectNode();
        body.put("model", properties.getEmbeddingEndpointName());
        body.put("input", text);

        try {
            String responseBody = webClient.post()
                    .uri(invocationUrl)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(body)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            if (responseBody == null || responseBody.isBlank()) {
                throw new IllegalStateException("Empty response from Databricks embedding endpoint");
            }

            JsonNode root = objectMapper.readTree(responseBody);
            return parseEmbedding(root);
        } catch (WebClientResponseException e) {
            throw new IllegalStateException(
                    "Databricks embedding invocation failed (" + e.getStatusCode() + "): "
                            + e.getResponseBodyAsString(),
                    e);
        } catch (Exception e) {
            throw new IllegalStateException("Databricks embedding invocation failed: " + e.getMessage(), e);
        }
    }

    private float[] parseEmbedding(JsonNode root) {
        JsonNode data = root.path("data");
        if (data.isArray() && !data.isEmpty()) {
            JsonNode embeddingNode = data.get(0).path("embedding");
            return toFloatArray(embeddingNode);
        }

        JsonNode predictions = root.path("predictions");
        if (predictions.isArray() && !predictions.isEmpty()) {
            return toFloatArray(predictions.get(0));
        }

        if (root.isArray() && !root.isEmpty()) {
            return toFloatArray(root.get(0));
        }

        throw new IllegalStateException("Unrecognized Databricks embedding response format: " + root);
    }

    private float[] toFloatArray(JsonNode embeddingNode) {
        if (!embeddingNode.isArray()) {
            throw new IllegalStateException("Expected embedding array but got: " + embeddingNode);
        }
        float[] vector = new float[embeddingNode.size()];
        for (int i = 0; i < embeddingNode.size(); i++) {
            vector[i] = (float) embeddingNode.get(i).asDouble();
        }
        return vector;
    }
}
