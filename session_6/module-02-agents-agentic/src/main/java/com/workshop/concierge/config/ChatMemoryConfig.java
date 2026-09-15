package com.workshop.concierge.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import dev.langchain4j.memory.chat.ChatMemoryProvider;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;

/**
 * Short-term memory configuration: a sliding window of the last N messages, bound to the
 * session ID supplied by the client ({@code @MemoryId} on the agent methods).
 */
@Configuration
public class ChatMemoryConfig {

    @Bean
    public ChatMemoryProvider chatMemoryProvider(ConciergeProperties properties) {
        int windowSize = properties.getChatMemory().getWindowSize();
        return sessionId -> MessageWindowChatMemory.builder()
                .id(sessionId)
                .maxMessages(windowSize)
                .build();
    }
}
