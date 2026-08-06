package com.owasp.aiassistant.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;

public record ChatRequest(
        @NotBlank String message,
        String conversationId,
        @Valid CodeAttachment codeToReview
) {
}
