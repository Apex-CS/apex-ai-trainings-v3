package com.acme.pdfingestion.dto;

import java.util.List;

public record EmbeddingBatchResponse(
        String fileName,
        Integer totalChunks,
        List<EmbeddingResponse> embeddings
) {
}
