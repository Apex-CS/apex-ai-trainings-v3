package com.owasp.aiassistant.exception;

/**
 * Raised when an external tool cannot be reached (SSL, DNS, timeouts, etc.).
 * Treated as a soft failure: the agent may continue with other tools.
 */
public class ToolConnectivityException extends RuntimeException {

    private final String toolName;

    public ToolConnectivityException(String toolName, String message, Throwable cause) {
        super(message, cause);
        this.toolName = toolName;
    }

    public ToolConnectivityException(String toolName, String message) {
        super(message);
        this.toolName = toolName;
    }

    public String getToolName() {
        return toolName;
    }
}
