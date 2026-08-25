package com.acme.pdfingestion.dto;

public record ExtractedTextResponse(
        String fileName,
        Integer characterCount,
        String extractedText
) {
}
