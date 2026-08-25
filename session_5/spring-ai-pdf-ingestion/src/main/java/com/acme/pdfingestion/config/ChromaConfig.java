package com.acme.pdfingestion.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.ai.chroma.vectorstore.ChromaApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class ChromaConfig {

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
}
