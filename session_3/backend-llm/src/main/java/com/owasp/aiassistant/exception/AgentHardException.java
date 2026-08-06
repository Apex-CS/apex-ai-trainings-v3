package com.owasp.aiassistant.exception;

/**
 * Fatal agent failure (e.g. LLM unavailable). Surfaces as a hard API error to the client.
 */
public class AgentHardException extends RuntimeException {

    public AgentHardException(String message) {
        super(message);
    }

    public AgentHardException(String message, Throwable cause) {
        super(message, cause);
    }
}
