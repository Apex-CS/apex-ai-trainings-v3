package com.workshop.concierge.config;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.output.Response;

class FailoverChatLanguageModelTest {

    @Test
    void shouldExposeConfiguredFallbackModelOrder() {
        ConciergeProperties properties = new ConciergeProperties();
        properties.getGemini().setChatModel("gemini-3.5-flash");
        properties.getGemini().setFallbackChatModel("gemini-3.6-flash");
        properties.getGemini().setSecondFallbackChatModel("gemini-3.7-flash");

        FailoverChatLanguageModel model = new FailoverChatLanguageModel(
                new AlwaysFailModel(),
                new AlwaysFailModel(),
                new AlwaysFailModel(),
                properties,
                new ObjectMapper());

        assertEquals(
                List.of("gemini-3.5-flash", "gemini-3.6-flash", "gemini-3.7-flash"),
                model.orderedFallbackModelNames());
    }

    private static final class AlwaysFailModel implements ChatLanguageModel {
        @Override
        public Response<AiMessage> generate(List<ChatMessage> messages) {
            throw new RuntimeException("UNAVAILABLE: model is currently experiencing high demand");
        }
    }
}
