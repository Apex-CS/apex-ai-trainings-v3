package com.workshop.mcp.module01.config;

import com.workshop.mcp.module01.tools.CalculatorTools;
import com.workshop.mcp.module01.tools.EchoTools;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * MCP Server Tool Registration — Module 01 Foundations.
 *
 * <p>Registers all @Tool-annotated service classes with the MCP runtime via
 * {@link MethodToolCallbackProvider}. The provider uses reflection to:
 * <ol>
 *   <li>Discover methods annotated with @Tool</li>
 *   <li>Generate JSON Schema from @ToolParam annotations</li>
 *   <li>Create ToolCallback objects the MCP engine dispatches at tools/call time</li>
 * </ol>
 */
@Configuration
public class McpServerConfig {

    @Bean
    public ToolCallbackProvider calculatorToolCallbacks(CalculatorTools calculatorTools) {
        return MethodToolCallbackProvider.builder()
                .toolObjects(calculatorTools)
                .build();
    }

    @Bean
    public ToolCallbackProvider echoToolCallbacks(EchoTools echoTools) {
        return MethodToolCallbackProvider.builder()
                .toolObjects(echoTools)
                .build();
    }
}
