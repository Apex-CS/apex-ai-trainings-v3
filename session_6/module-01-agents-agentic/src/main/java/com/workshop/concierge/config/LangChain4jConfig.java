package com.workshop.concierge.config;

import java.time.Duration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.googleai.GoogleAiEmbeddingModel;
import dev.langchain4j.model.googleai.GoogleAiGeminiChatModel;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore;

/**
 * Low-level LangChain4j building blocks: chat model, embedding model and the
 * long-term-memory vector store. Backed by Google AI Gemini; swap the beans here to change providers.
 */
@Configuration
public class LangChain4jConfig {

    @Bean
    public ChatLanguageModel chatLanguageModel(ConciergeProperties properties) {
        ChatLanguageModel primary = GoogleAiGeminiChatModel.builder()
                .apiKey(properties.getGemini().getApiKey())
                .modelName(properties.getGemini().getChatModel())
                .maxRetries(properties.getGemini().getMaxRetries())
                .timeout(Duration.ofSeconds(properties.getGemini().getTimeoutSeconds()))
                .logRequestsAndResponses(true)
                .build();
        ChatLanguageModel fallback = GoogleAiGeminiChatModel.builder()
            .apiKey(properties.getGemini().getApiKey())
            .modelName(properties.getGemini().getFallbackChatModel())
            .maxRetries(properties.getGemini().getMaxRetries())
            .timeout(Duration.ofSeconds(properties.getGemini().getTimeoutSeconds()))
            .logRequestsAndResponses(true)
            .build();
        ChatLanguageModel secondFallback = GoogleAiGeminiChatModel.builder()
            .apiKey(properties.getGemini().getApiKey())
            .modelName(properties.getGemini().getSecondFallbackChatModel())
            .maxRetries(properties.getGemini().getMaxRetries())
            .timeout(Duration.ofSeconds(properties.getGemini().getTimeoutSeconds()))
            .logRequestsAndResponses(true)
            .build();
        return new FailoverChatLanguageModel(primary, fallback, secondFallback, properties, new com.fasterxml.jackson.databind.ObjectMapper());
    }

    @Bean
    public EmbeddingModel embeddingModel(ConciergeProperties properties) {
        return GoogleAiEmbeddingModel.builder()
                .apiKey(properties.getGemini().getApiKey())
                .modelName(properties.getGemini().getEmbeddingModel())
                .maxRetries(properties.getGemini().getMaxRetries())
                .timeout(Duration.ofSeconds(properties.getGemini().getTimeoutSeconds()))
                .build();
    }

    @Bean
    public EmbeddingStore<TextSegment> embeddingStore() {
        // In-memory long-term memory store. Swap for PgVectorEmbeddingStore for real persistence.
        return new InMemoryEmbeddingStore<>();
    }
}
