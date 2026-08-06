package com.owasp.aiassistant.tools;

import com.owasp.aiassistant.agent.AgentWarningContext;
import com.owasp.aiassistant.exception.ToolConnectivityException;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;

public class ResilientToolCallback implements ToolCallback {

    private final ToolCallback delegate;
    private final AgentWarningContext warningContext;

    public ResilientToolCallback(ToolCallback delegate, AgentWarningContext warningContext) {
        this.delegate = delegate;
        this.warningContext = warningContext;
    }

    @Override
    public ToolDefinition getToolDefinition() {
        return delegate.getToolDefinition();
    }

    @Override
    public String call(String toolInput) {
        String toolName = delegate.getToolDefinition().name();
        try {
            return delegate.call(toolInput);
        } catch (ToolConnectivityException e) {
            warningContext.add(ToolErrorClassifier.formatWarning(toolName, e));
            return unavailableMessage(toolName, e);
        } catch (Exception e) {
            if (ToolErrorClassifier.isConnectivityError(e)) {
                warningContext.add(ToolErrorClassifier.formatWarning(toolName, e));
                return unavailableMessage(toolName, e);
            }
            return "Tool " + toolName + " error: " + ToolErrorClassifier.rootCause(e).getMessage();
        }
    }

    private static String unavailableMessage(String toolName, Throwable error) {
        String detail = ToolErrorClassifier.rootCause(error).getMessage();
        return "The " + toolName + " tool is temporarily unavailable"
                + (detail != null ? " (" + detail + ")" : "")
                + ". Try another tool or answer from available information.";
    }
}
