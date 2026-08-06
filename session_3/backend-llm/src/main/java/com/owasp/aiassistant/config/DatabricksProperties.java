package com.owasp.aiassistant.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.databricks")
public class DatabricksProperties {

    private String host;
    private String token;
    private String endpointName;
    private String embeddingEndpointName;
    /**
     * Optional full chat invocation URL (e.g. route-optimized endpoint).
     */
    private String chatInvocationUrl;
    /**
     * Optional full embedding invocation URL (e.g. route-optimized endpoint).
     */
    private String embeddingInvocationUrl;
    private double temperature = 0.2;
    private int maxTokens = 4096;

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getEndpointName() {
        return endpointName;
    }

    public void setEndpointName(String endpointName) {
        this.endpointName = endpointName;
    }

    public String getEmbeddingEndpointName() {
        return embeddingEndpointName;
    }

    public void setEmbeddingEndpointName(String embeddingEndpointName) {
        this.embeddingEndpointName = embeddingEndpointName;
    }

    public String getChatInvocationUrl() {
        return chatInvocationUrl;
    }

    public void setChatInvocationUrl(String chatInvocationUrl) {
        this.chatInvocationUrl = chatInvocationUrl;
    }

    public String getEmbeddingInvocationUrl() {
        return embeddingInvocationUrl;
    }

    public void setEmbeddingInvocationUrl(String embeddingInvocationUrl) {
        this.embeddingInvocationUrl = embeddingInvocationUrl;
    }

    public double getTemperature() {
        return temperature;
    }

    public void setTemperature(double temperature) {
        this.temperature = temperature;
    }

    public int getMaxTokens() {
        return maxTokens;
    }

    public void setMaxTokens(int maxTokens) {
        this.maxTokens = maxTokens;
    }

    public String resolveChatInvocationUrl() {
        if (chatInvocationUrl != null && !chatInvocationUrl.isBlank()) {
            return chatInvocationUrl.trim();
        }
        requireEndpointName(endpointName, "LLM_ENDPOINT_NAME");
        return normalizedHost() + "/serving-endpoints/chat/completions";
    }

    public String resolveEmbeddingInvocationUrl() {
        if (embeddingInvocationUrl != null && !embeddingInvocationUrl.isBlank()) {
            return embeddingInvocationUrl.trim();
        }
        requireEndpointName(embeddingEndpointName, "EMBEDDING_ENDPOINT_NAME");
        return normalizedHost() + "/serving-endpoints/embeddings";
    }

    private String normalizedHost() {
        if (host == null || host.isBlank()) {
            throw new IllegalStateException("app.databricks.host (DATABRICKS_HOST) is required");
        }
        return host.endsWith("/") ? host.substring(0, host.length() - 1) : host;
    }

    private void requireEndpointName(String endpoint, String envName) {
        if (endpoint == null || endpoint.isBlank()) {
            throw new IllegalStateException("Databricks endpoint name is required (" + envName + ")");
        }
    }
}
