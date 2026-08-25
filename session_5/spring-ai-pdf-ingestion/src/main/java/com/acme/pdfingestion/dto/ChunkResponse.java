package com.acme.pdfingestion.dto;

public record ChunkResponse(
        String documentId,
        Integer chunkNumber,
        Integer textLength,
        String text
) {
}