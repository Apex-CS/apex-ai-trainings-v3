package com.owasp.aiassistant.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.owasp.aiassistant.databricks.DatabricksChatModel;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
@EnableConfigurationProperties(OutputJudgeProperties.class)
@ConditionalOnProperty(name = "app.guardrails.output-judge.enabled", havingValue = "true", matchIfMissing = true)
public class OutputJudgeConfig {

    @Bean
    @Qualifier("judgeChatModel")
    ChatModel judgeChatModel(
            WebClient.Builder webClientBuilder,
            DatabricksProperties databricksProperties,
            OutputJudgeProperties outputJudgeProperties,
            ObjectMapper objectMapper) {
        DatabricksProperties judgeProperties = resolveJudgeProperties(databricksProperties, outputJudgeProperties);
        return new DatabricksChatModel(webClientBuilder, judgeProperties, objectMapper);
    }

    static DatabricksProperties resolveJudgeProperties(
            DatabricksProperties mainProperties,
            OutputJudgeProperties outputJudgeProperties) {
        DatabricksProperties judgeProperties = new DatabricksProperties();
        OutputJudgeProperties.Model model = outputJudgeProperties.getModel();

        judgeProperties.setHost(firstNonBlank(model.getHost(), mainProperties.getHost()));
        judgeProperties.setToken(firstNonBlank(model.getToken(), mainProperties.getToken()));
        judgeProperties.setEndpointName(firstNonBlank(model.getEndpointName(), mainProperties.getEndpointName()));
        judgeProperties.setChatInvocationUrl(firstNonBlank(
                model.getChatInvocationUrl(),
                mainProperties.getChatInvocationUrl()));

        if (model.getTemperature() != null) {
            judgeProperties.setTemperature(model.getTemperature());
        } else {
            judgeProperties.setTemperature(mainProperties.getTemperature());
        }

        if (model.getMaxTokens() != null) {
            judgeProperties.setMaxTokens(model.getMaxTokens());
        } else {
            judgeProperties.setMaxTokens(mainProperties.getMaxTokens());
        }

        return judgeProperties;
    }

    private static String firstNonBlank(String preferred, String fallback) {
        if (preferred != null && !preferred.isBlank()) {
            return preferred.trim();
        }
        return fallback;
    }
}
