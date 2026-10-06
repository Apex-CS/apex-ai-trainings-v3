package com.workshop.concierge.agent;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Base64;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.workshop.concierge.config.ConciergeProperties;

/** Direct Gemini fallback for transient capacity failures in the older LangChain4j adapter. */
@Component
public class GeminiVisionHttpClient {

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final ConciergeProperties properties;

    public GeminiVisionHttpClient(ObjectMapper objectMapper, ConciergeProperties properties) {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(properties.getGemini().getTimeoutSeconds()))
                .build();
        this.objectMapper = objectMapper;
        this.properties = properties;
    }

    public String analyze(byte[] imageBytes, String mimeType, String instructions) {
        try {
            ObjectNode request = objectMapper.createObjectNode();
            ArrayNode contents = request.putArray("contents");
            ObjectNode content = contents.addObject();
            ArrayNode parts = content.putArray("parts");
            ObjectNode textPart = parts.addObject();
            textPart.put("text", instructions);
            ObjectNode inlineData = parts.addObject();
            inlineData.putObject("inline_data")
                    .put("mime_type", mimeType == null ? "image/jpeg" : mimeType)
                    .put("data", Base64.getEncoder().encodeToString(imageBytes));

            String model = properties.getGemini().getFallbackChatModel();
            String endpoint = "https://generativelanguage.googleapis.com/v1beta/models/"
                    + model + ":generateContent?key=" + properties.getGemini().getApiKey();
            HttpRequest httpRequest = HttpRequest.newBuilder(URI.create(endpoint))
                    .timeout(Duration.ofSeconds(properties.getGemini().getTimeoutSeconds()))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(request)))
                    .build();

            HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 400) {
                throw new IllegalStateException("Gemini vision fallback returned HTTP " + response.statusCode());
            }
            JsonNode body = objectMapper.readTree(response.body());
            return body.path("candidates").path(0).path("content").path("parts").path(0).path("text").asText();
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Gemini vision fallback was interrupted", exception);
        } catch (Exception exception) {
            throw new IllegalStateException("Gemini vision fallback failed", exception);
        }
    }
}
