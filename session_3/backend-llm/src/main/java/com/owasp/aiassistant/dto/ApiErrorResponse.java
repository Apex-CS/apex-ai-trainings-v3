package com.owasp.aiassistant.dto;

public record ApiErrorResponse(
        String error,
        String type
) {
}
