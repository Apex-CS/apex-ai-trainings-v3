package com.owasp.aiassistant.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.owasp.aiassistant.databricks.DatabricksChatModel;
import com.owasp.aiassistant.databricks.DatabricksEmbeddingModel;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
@EnableConfigurationProperties(DatabricksProperties.class)
public class DatabricksConfig {

    @Bean
    @Primary
    ChatModel databricksChatModel(
            WebClient.Builder webClientBuilder,
            DatabricksProperties properties,
            ObjectMapper objectMapper) {
        return new DatabricksChatModel(webClientBuilder, properties, objectMapper);
    }

    @Bean
    @Primary
    @ConditionalOnProperty(name = "app.rag.enabled", havingValue = "true")
    EmbeddingModel databricksEmbeddingModel(
            WebClient.Builder webClientBuilder,
            DatabricksProperties properties,
            ObjectMapper objectMapper) {
        return new DatabricksEmbeddingModel(webClientBuilder, properties, objectMapper);
    }
}
