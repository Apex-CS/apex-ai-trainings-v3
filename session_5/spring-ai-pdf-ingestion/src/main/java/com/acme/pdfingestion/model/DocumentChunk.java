package com.acme.pdfingestion.model;

public record DocumentChunk(
        String id,
        Integer chunkNumber,
        String text
) {
}
