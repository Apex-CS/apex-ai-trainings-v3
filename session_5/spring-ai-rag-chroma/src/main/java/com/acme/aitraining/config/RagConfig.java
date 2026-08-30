package com.acme.aitraining.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.ai.chroma.vectorstore.ChromaApi;
import org.springframework.ai.chroma.vectorstore.ChromaVectorStore;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class RagConfig {

  @Bean
  public ChromaApi chromaApi(
          RestClient.Builder restClientBuilder,
          ObjectMapper objectMapper) {

    return new ChromaApi(
            "http://localhost:8000",
            restClientBuilder,
            objectMapper
    );
  }

  @Bean
  public VectorStore vectorStore(
          ChromaApi chromaApi,
          EmbeddingModel embeddingModel) {

    return ChromaVectorStore.builder(
                    chromaApi,
                    embeddingModel)
            .tenantName("default_tenant")
            .databaseName("default_database")
            .collectionName("demo-rag")
            .initializeSchema(false)
            .build();
  }

  @Bean("fastShowVectorStore")
  public VectorStore fastShowVectorStore(
          ChromaApi chromaApi,
          EmbeddingModel embeddingModel) {

    return ChromaVectorStore.builder(
                    chromaApi,
                    embeddingModel)
            .tenantName("default_tenant")
            .databaseName("default_database")
            .collectionName("FastShow-collection")
            .initializeSchema(false)
            .build();
  }
}