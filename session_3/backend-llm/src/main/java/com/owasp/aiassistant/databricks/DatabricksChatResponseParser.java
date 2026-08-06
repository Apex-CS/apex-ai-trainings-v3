package com.owasp.aiassistant.databricks;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.ai.chat.messages.AssistantMessage;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

final class DatabricksChatResponseParser {

    private DatabricksChatResponseParser() {
    }

    static AssistantMessage parseAssistantMessage(JsonNode root) {
        JsonNode choices = root.path("choices");
        if (choices.isArray() && !choices.isEmpty()) {
            JsonNode message = choices.get(0).path("message");
            String content = extractMessageContent(message.path("content"));
            JsonNode toolCalls = message.path("tool_calls");
            if (toolCalls.isArray() && !toolCalls.isEmpty()) {
                List<AssistantMessage.ToolCall> calls = new ArrayList<>();
                for (JsonNode toolCall : toolCalls) {
                    calls.add(new AssistantMessage.ToolCall(
                            toolCall.path("id").asText(),
                            "function",
                            toolCall.path("function").path("name").asText(),
                            toolCall.path("function").path("arguments").asText("{}")));
                }
                return new AssistantMessage(content, Map.of(), calls);
            }
            return new AssistantMessage(content);
        }

        if (root.has("predictions")) {
            return new AssistantMessage(root.path("predictions").get(0).asText(""));
        }

        throw new IllegalStateException("Unrecognized Databricks chat response format: " + root);
    }

    static String extractMessageContent(JsonNode contentNode) {
        if (contentNode.isMissingNode() || contentNode.isNull()) {
            return "";
        }
        if (contentNode.isTextual()) {
            return contentNode.asText("");
        }
        if (contentNode.isArray()) {
            StringBuilder answer = new StringBuilder();
            for (JsonNode part : contentNode) {
                if (!"text".equals(part.path("type").asText())) {
                    continue;
                }
                appendText(answer, part.path("text").asText(""));
            }
            return answer.toString().trim();
        }
        return contentNode.asText("");
    }

    private static void appendText(StringBuilder builder, String text) {
        if (text == null || text.isBlank()) {
            return;
        }
        if (!builder.isEmpty()) {
            builder.append('\n');
        }
        builder.append(text);
    }
}
