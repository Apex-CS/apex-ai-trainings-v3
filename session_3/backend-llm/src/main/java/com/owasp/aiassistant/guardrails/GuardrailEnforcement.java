package com.owasp.aiassistant.guardrails;

public enum GuardrailEnforcement {
    /** Blocks the request or response when violated. */
    HARD,
    /** Injected as guidance; violations surface as warnings, not blocks. */
    SOFT
}
