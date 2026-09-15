package com.workshop.concierge.guardrail;

/**
 * Thrown by {@link AllergyGuardrail} when a generated recipe cannot be made safe for the user
 * even after the configured number of automatic re-prompt retries.
 */
public class AllergyViolationException extends RuntimeException {

    public AllergyViolationException(String message) {
        super(message);
    }
}
