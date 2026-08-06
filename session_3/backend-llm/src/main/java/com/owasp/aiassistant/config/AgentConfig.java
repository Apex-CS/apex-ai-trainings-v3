package com.owasp.aiassistant.config;

import com.owasp.aiassistant.agent.AgentWarningContext;
import com.owasp.aiassistant.tools.AgentTools;
import com.owasp.aiassistant.tools.ResilientToolCallback;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Arrays;
import java.util.List;

@Configuration
public class AgentConfig {

    @Bean
    ToolCallbackProvider toolCallbackProvider(AgentTools agentTools) {
        return MethodToolCallbackProvider.builder()
                .toolObjects(agentTools)
                .build();
    }

    @Bean
    List<ToolCallback> toolCallbacks(
            ToolCallbackProvider toolCallbackProvider,
            AgentWarningContext warningContext) {
        return Arrays.stream(toolCallbackProvider.getToolCallbacks())
                .<ToolCallback>map(callback -> new ResilientToolCallback(callback, warningContext))
                .toList();
    }

    @Bean
    ChatClient chatClient(ChatModel chatModel) {
        return ChatClient.builder(chatModel).build();
    }
}
