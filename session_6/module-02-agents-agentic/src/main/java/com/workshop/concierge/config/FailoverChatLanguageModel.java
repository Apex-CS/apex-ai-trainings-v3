package com.workshop.concierge.config;

import java.util.List;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.output.Response;

/** Uses a secondary model when Gemini reports temporary capacity exhaustion. */
public class FailoverChatLanguageModel implements ChatLanguageModel {

    private final ChatLanguageModel primary;
    private final List<ChatLanguageModel> models;
    private final ConciergeProperties properties;
    private final ObjectMapper objectMapper;

    public FailoverChatLanguageModel(ChatLanguageModel primary, ChatLanguageModel fallback,
                                     ChatLanguageModel secondFallback,
                                     ConciergeProperties properties,
                                     ObjectMapper objectMapper) {
        this.primary = primary;
        this.models = List.of(primary, fallback, secondFallback);
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    @Override
    public Response<AiMessage> generate(List<ChatMessage> messages) {
        RuntimeException lastCapacityFailure = null;
        for (ChatLanguageModel model : models) {
            try {
                return model.generate(messages);
            } catch (RuntimeException exception) {
                if (!isTransientFailure(exception)) {
                    throw exception;
                }
                lastCapacityFailure = exception;
            }
        }
        return directGeminiFallback(messages, lastCapacityFailure);
    }

    private Response<AiMessage> directGeminiFallback(List<ChatMessage> messages, RuntimeException cause) {
        try {
            ObjectNode request = objectMapper.createObjectNode();
            ArrayNode contents = request.putArray("contents");
            StringBuilder prompt = new StringBuilder();
            for (ChatMessage message : messages) {
                if (message instanceof dev.langchain4j.data.message.SystemMessage systemMessage) {
                    request.putObject("systemInstruction").putArray("parts")
                            .addObject().put("text", systemMessage.text());
                } else if (message instanceof dev.langchain4j.data.message.UserMessage userMessage) {
                    prompt.append(userMessage.text()).append("\n");
                }
            }
            contents.addObject().putArray("parts").addObject().put("text", prompt.toString());
            String endpoint = "https://generativelanguage.googleapis.com/v1beta/models/"
                    + properties.getGemini().getChatModel() + ":generateContent?key="
                    + properties.getGemini().getApiKey();
            HttpRequest requestMessage = HttpRequest.newBuilder(URI.create(endpoint))
                    .timeout(Duration.ofSeconds(properties.getGemini().getTimeoutSeconds()))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(request)))
                    .build();
            HttpResponse<String> response = HttpClient.newHttpClient()
                    .send(requestMessage, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 400) {
                throw cause;
            }
            JsonNode body = objectMapper.readTree(response.body());
            String text = body.path("candidates").path(0).path("content").path("parts")
                    .path(0).path("text").asText();
            return Response.from(AiMessage.from(text));
        } catch (Exception exception) {
            throw cause;
        }
    }

    private boolean isTransientFailure(Throwable exception) {
        Throwable current = exception;
        while (current != null) {
            String message = current.getMessage();
            if (message != null && (message.contains("503") || message.contains("UNAVAILABLE")
                    || message.toLowerCase().contains("high demand")
                    || message.toLowerCase().contains("timeout"))) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }
}