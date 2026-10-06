package com.workshop.concierge.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record MealPlanRequest(
        @NotBlank String userId,
        @NotBlank String sessionId,
        @Valid @NotNull TargetMacros targetMacros,
        @Positive int days
) {
    public MealPlanRequest {
        if (targetMacros == null) {
            throw new IllegalArgumentException("targetMacros is required");
        }
    }
}
