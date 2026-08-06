package com.owasp.aiassistant.agent;

import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.ai.chat.messages.UserMessage;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class AgentMessageTraceMapper {

    private AgentMessageTraceMapper() {
    }

    static List<Map<String, Object>> toTraceMessages(List<Message> messages) {
        List<Map<String, Object>> traced = new ArrayList<>();
        for (Message message : messages) {
            traced.add(toTraceMessage(message));
        }
        return traced;
    }

    static List<Map<String, Object>> extractToolCalls(List<Message> messages) {
        List<Map<String, Object>> toolCalls = new ArrayList<>();
        for (Message message : messages) {
            if (message instanceof AssistantMessage assistantMessage && assistantMessage.hasToolCalls()) {
                assistantMessage.getToolCalls().forEach(toolCall -> {
                    Map<String, Object> entry = new LinkedHashMap<>();
                    entry.put("id", toolCall.id());
                    entry.put("name", toolCall.name());
                    entry.put("arguments", toolCall.arguments());
                    toolCalls.add(entry);
                });
            }
        }
        return toolCalls;
    }

    private static Map<String, Object> toTraceMessage(Message message) {
        Map<String, Object> entry = new LinkedHashMap<>();
        entry.put("role", roleFor(message));
        entry.put("content", contentFor(message));
        if (message instanceof AssistantMessage assistantMessage && assistantMessage.hasToolCalls()) {
            entry.put("tool_calls", extractToolCalls(List.of(assistantMessage)));
        }
        if (message instanceof ToolResponseMessage toolResponseMessage) {
            entry.put(
                    "tool_responses",
                    toolResponseMessage.getResponses().stream()
                            .map(response -> Map.<String, Object>of(
                                    "id", response.id(),
                                    "name", response.name(),
                                    "response", response.responseData()))
                            .toList());
        }
        return entry;
    }

    private static String roleFor(Message message) {
        if (message instanceof UserMessage) {
            return "user";
        }
        if (message instanceof AssistantMessage) {
            return "assistant";
        }
        if (message instanceof SystemMessage) {
            return "system";
        }
        if (message instanceof ToolResponseMessage) {
            return "tool";
        }
        return message.getClass().getSimpleName();
    }

    private static String contentFor(Message message) {
        if (message instanceof UserMessage userMessage) {
            return userMessage.getText();
        }
        if (message instanceof AssistantMessage assistantMessage) {
            return assistantMessage.getText();
        }
        if (message instanceof SystemMessage systemMessage) {
            return systemMessage.getText();
        }
        if (message instanceof ToolResponseMessage toolResponseMessage) {
            return toolResponseMessage.getResponses().stream()
                    .map(ToolResponseMessage.ToolResponse::responseData)
                    .reduce((left, right) -> left + "\n" + right)
                    .orElse("");
        }
        return message.toString();
    }
}
