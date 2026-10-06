package com.workshop.concierge.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Binds the {@code concierge.*} properties from application.yml.
 */
@ConfigurationProperties(prefix = "concierge")
public class ConciergeProperties {

    private final Gemini gemini = new Gemini();
    private final ChatMemory chatMemory = new ChatMemory();
    private final Guardrails guardrails = new Guardrails();
    private final Ui ui = new Ui();

    public Gemini getGemini() {
        return gemini;
    }

    public ChatMemory getChatMemory() {
        return chatMemory;
    }

    public Guardrails getGuardrails() {
        return guardrails;
    }

    public Ui getUi() {
        return ui;
    }

    public static class Gemini {
        private String apiKey;
        private String chatModel;
        private String fallbackChatModel;
        private String secondFallbackChatModel;
        private String embeddingModel;
        private String visionModel;
        private int timeoutSeconds = 120;
        private int maxRetries = 3;

        public String getApiKey() {
            return apiKey;
        }

        public void setApiKey(String apiKey) {
            this.apiKey = apiKey;
        }

        public String getChatModel() {
            return chatModel;
        }

        public void setChatModel(String chatModel) {
            this.chatModel = chatModel;
        }

        public String getFallbackChatModel() {
            return fallbackChatModel;
        }

        public void setFallbackChatModel(String fallbackChatModel) {
            this.fallbackChatModel = fallbackChatModel;
        }

        public String getSecondFallbackChatModel() {
            return secondFallbackChatModel;
        }

        public void setSecondFallbackChatModel(String secondFallbackChatModel) {
            this.secondFallbackChatModel = secondFallbackChatModel;
        }

        public String getEmbeddingModel() {
            return embeddingModel;
        }

        public void setEmbeddingModel(String embeddingModel) {
            this.embeddingModel = embeddingModel;
        }

        public String getVisionModel() {
            return visionModel;
        }

        public void setVisionModel(String visionModel) {
            this.visionModel = visionModel;
        }

        public int getTimeoutSeconds() {
            return timeoutSeconds;
        }

        public void setTimeoutSeconds(int timeoutSeconds) {
            this.timeoutSeconds = timeoutSeconds;
        }

        public int getMaxRetries() {
            return maxRetries;
        }

        public void setMaxRetries(int maxRetries) {
            this.maxRetries = maxRetries;
        }
    }

    public static class ChatMemory {
        private int windowSize = 10;

        public int getWindowSize() {
            return windowSize;
        }

        public void setWindowSize(int windowSize) {
            this.windowSize = windowSize;
        }
    }

    public static class Guardrails {
        private int maxRecipeRetries = 2;

        public int getMaxRecipeRetries() {
            return maxRecipeRetries;
        }

        public void setMaxRecipeRetries(int maxRecipeRetries) {
            this.maxRecipeRetries = maxRecipeRetries;
        }
    }

    public static class Ui {
        private int port = 8501;

        public int getPort() {
            return port;
        }

        public void setPort(int port) {
            this.port = port;
        }
    }
}
