package com.workshop.concierge.dto;

import jakarta.validation.constraints.Min;

public record TargetMacros(
        @Min(0) int calories,
        @Min(0) int protein
) {
}
