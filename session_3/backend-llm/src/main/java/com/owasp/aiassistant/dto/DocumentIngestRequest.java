package com.owasp.aiassistant.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;

public record DocumentIngestRequest(
        @NotBlank String documentId,
        String title,
        String sourcePath,
        String html,
        @JsonProperty("html_file_location") String htmlFileLocation
) {
    @AssertTrue(message = "Either html or html_file_location must be provided")
    @JsonIgnore
    public boolean isHtmlSourceProvided() {
        return isNotBlank(html) || isNotBlank(htmlFileLocation);
    }

    private static boolean isNotBlank(String value) {
        return value != null && !value.isBlank();
    }
}
