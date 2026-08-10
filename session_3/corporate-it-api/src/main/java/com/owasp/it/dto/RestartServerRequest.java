package com.owasp.it.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RestartServerRequest(
        @NotBlank
        @Size(max = 60)
        String appName) {
}
