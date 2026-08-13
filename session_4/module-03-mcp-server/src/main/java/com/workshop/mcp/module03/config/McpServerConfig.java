package com.workshop.mcp.module03.config;

import com.workshop.mcp.module03.tools.CustomerTools;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * MCP Server configuration for Module 03.
 * Registers CustomerTools with the MCP runtime.
 */
@Configuration
public class McpServerConfig {

    @Bean
    public ToolCallbackProvider customerToolCallbacks(CustomerTools customerTools) {
        return MethodToolCallbackProvider.builder()
                .toolObjects(customerTools)
                .build();
    }
}
