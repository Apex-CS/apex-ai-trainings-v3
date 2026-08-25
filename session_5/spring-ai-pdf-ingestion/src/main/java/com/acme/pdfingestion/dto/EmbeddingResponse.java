package com.acme.pdfingestion.dto;

import java.util.List;

public record EmbeddingResponse(
        String documentId,
        Integer chunkNumber,
        Integer dimensions,
        List<Float> embeddingPreview
) {
}
