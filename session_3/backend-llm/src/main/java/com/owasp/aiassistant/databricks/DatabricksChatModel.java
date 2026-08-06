package com.owasp.aiassistant.databricks;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.owasp.aiassistant.config.DatabricksProperties;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.MessageType;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.model.tool.ToolCallingChatOptions;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Flux;

import java.util.List;

public class DatabricksChatModel implements ChatModel {

    private final WebClient webClient;
    private final DatabricksProperties properties;
    private final ObjectMapper objectMapper;
    private final String invocationUrl;

    public DatabricksChatModel(
            WebClient.Builder webClientBuilder,
            DatabricksProperties properties,
            ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.invocationUrl = properties.resolveChatInvocationUrl();
        this.webClient = webClientBuilder
                .defaultHeader("Authorization", "Bearer " + properties.getToken())
                .build();
    }

    @Override
    public ChatResponse call(Prompt prompt) {
        ObjectNode requestBody = buildRequestBody(prompt);

        try {
            String responseBody = webClient.post()
                    .uri(invocationUrl)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            if (responseBody == null || responseBody.isBlank()) {
                throw new IllegalStateException("Empty response from Databricks chat endpoint");
            }

            JsonNode root = objectMapper.readTree(responseBody);
            AssistantMessage assistantMessage = DatabricksChatResponseParser.parseAssistantMessage(root);
            return new ChatResponse(List.of(new Generation(assistantMessage)));
        } catch (WebClientResponseException e) {
            throw new IllegalStateException(
                    "Databricks chat invocation failed (" + e.getStatusCode() + "): " + e.getResponseBodyAsString(), e);
        } catch (Exception e) {
            throw new IllegalStateException("Databricks chat invocation failed: " + e.getMessage(), e);
        }
    }

    @Override
    public Flux<ChatResponse> stream(Prompt prompt) {
        return Flux.just(call(prompt));
    }

    @Override
    public ChatOptions getDefaultOptions() {
        return ChatOptions.builder()
                .model(properties.getEndpointName())
                .temperature(properties.getTemperature())
                .maxTokens(properties.getMaxTokens())
                .build();
    }

    private ObjectNode buildRequestBody(Prompt prompt) {
        ObjectNode body = objectMapper.createObjectNode();
        body.put("model", properties.getEndpointName());
        body.put("max_tokens", properties.getMaxTokens());
        body.put("temperature", properties.getTemperature());

        ArrayNode messages = body.putArray("messages");
        for (Message message : prompt.getInstructions()) {
            appendDatabricksMessages(messages, message);
        }

        if (prompt.getOptions() instanceof ToolCallingChatOptions toolOptions
                && toolOptions.getToolCallbacks() != null) {
            ArrayNode tools = body.putArray("tools");
            toolOptions.getToolCallbacks().forEach(callback -> {
                var definition = callback.getToolDefinition();
                ObjectNode tool = tools.addObject();
                tool.put("type", "function");
                ObjectNode function = tool.putObject("function");
                function.put("name", definition.name());
                function.put("description", definition.description());
                function.set("parameters", parseInputSchema(definition.inputSchema()));
            });
        }

        return body;
    }

    private JsonNode parseInputSchema(String inputSchema) {
        try {
            return objectMapper.readTree(inputSchema);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to parse tool input schema", e);
        }
    }

    private void appendDatabricksMessages(ArrayNode messages, Message message) {
        if (message instanceof AssistantMessage assistantMessage) {
            messages.add(toAssistantMessage(assistantMessage));
            return;
        }
        if (message instanceof ToolResponseMessage toolResponseMessage) {
            for (ToolResponseMessage.ToolResponse response : toolResponseMessage.getResponses()) {
                messages.add(toToolMessage(response));
            }
            return;
        }
        messages.add(toSimpleMessage(message));
    }

    private ObjectNode toSimpleMessage(Message message) {
        ObjectNode node = objectMapper.createObjectNode();
        node.put("role", toRole(message));
        node.put("content", message.getText() != null ? message.getText() : "");
        return node;
    }

    private ObjectNode toAssistantMessage(AssistantMessage message) {
        ObjectNode node = objectMapper.createObjectNode();
        node.put("role", "assistant");

        String content = message.getText();
        if (content != null && !content.isBlank()) {
            node.put("content", content);
        } else if (!message.hasToolCalls()) {
            node.put("content", "");
        }

        if (message.hasToolCalls()) {
            ArrayNode toolCalls = node.putArray("tool_calls");
            for (AssistantMessage.ToolCall toolCall : message.getToolCalls()) {
                ObjectNode call = toolCalls.addObject();
                call.put("id", toolCall.id());
                call.put("type", toolCall.type() != null ? toolCall.type() : "function");
                ObjectNode function = call.putObject("function");
                function.put("name", toolCall.name());
                function.put("arguments", toolCall.arguments() != null ? toolCall.arguments() : "{}");
            }
        }

        return node;
    }

    private ObjectNode toToolMessage(ToolResponseMessage.ToolResponse response) {
        ObjectNode node = objectMapper.createObjectNode();
        node.put("role", "tool");
        node.put("content", response.responseData() != null ? response.responseData() : "");
        node.put("tool_call_id", response.id());
        if (response.name() != null && !response.name().isBlank()) {
            node.put("name", response.name());
        }
        return node;
    }

    private static String toRole(Message message) {
        MessageType type = message.getMessageType();
        return switch (type) {
            case SYSTEM -> "system";
            case USER -> "user";
            case ASSISTANT -> "assistant";
            case TOOL -> "tool";
            default -> "user";
        };
    }

}
