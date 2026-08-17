package com.workshop.mcp.jira.config;

import com.workshop.mcp.jira.tools.JiraTools;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class McpServerConfig {

    @Bean
    public ToolCallbackProvider jiraToolCallbacks(JiraTools jiraTools) {
        return MethodToolCallbackProvider.builder()
                .toolObjects(jiraTools)
                .build();
    }
}
