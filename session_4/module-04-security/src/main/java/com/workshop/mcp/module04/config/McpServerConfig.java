package com.workshop.mcp.module04.config;

import com.workshop.mcp.module04.tools.SecureDeploymentTools;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class McpServerConfig {

    @Bean
    public ToolCallbackProvider deploymentToolCallbacks(SecureDeploymentTools deploymentTools) {
        return MethodToolCallbackProvider.builder()
                .toolObjects(deploymentTools)
                .build();
    }
}
