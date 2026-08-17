package com.workshop.mcp.module04.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.workshop.mcp.module04.tools.SecureDeploymentTools;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.definition.ToolDefinition;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Arrays;

@Configuration
public class McpServerConfig {

    @Bean
    public ToolCallbackProvider deploymentToolCallbacks(SecureDeploymentTools deploymentTools,
                                                        ObjectMapper objectMapper) {
        var provider = MethodToolCallbackProvider.builder()
                .toolObjects(deploymentTools)
                .build();

        // Spring AI 1.0.0 DefaultToolCallResultConverter calls JsonParser.toJson() on ALL return
        // types including String, double-encoding JSON string results (adds outer quotes).
        // Wrap each callback to detect and remove the extra JSON string encoding layer.
        var fixed = Arrays.stream(provider.getToolCallbacks())
                .map(cb -> new ToolCallback() {
                    @Override
                    public ToolDefinition getToolDefinition() {
                        return cb.getToolDefinition();
                    }

                    @Override
                    public String call(String toolInput) {
                        return undoubleEncode(cb.call(toolInput), objectMapper);
                    }

                    @Override
                    public String call(String toolInput, org.springframework.ai.chat.model.ToolContext context) {
                        return undoubleEncode(cb.call(toolInput, context), objectMapper);
                    }
                })
                .toList();

        return ToolCallbackProvider.from(fixed);
    }

    private static String undoubleEncode(String result, ObjectMapper objectMapper) {
        if (result != null && result.startsWith("\"")) {
            try {
                return objectMapper.readValue(result, String.class);
            } catch (Exception ignored) {
            }
        }
        return result;
    }
}

