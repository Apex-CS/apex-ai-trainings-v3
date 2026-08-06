package com.owasp.aiassistant.dto;

import jakarta.validation.constraints.NotBlank;

public record CodeAttachment(
        @NotBlank String filename,
        String contentType,
        @NotBlank String encoding,
        @NotBlank String data
) {
}
