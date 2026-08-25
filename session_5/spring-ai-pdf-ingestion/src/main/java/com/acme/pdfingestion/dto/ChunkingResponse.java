package com.acme.pdfingestion.dto;

import java.util.List;

public record ChunkingResponse(
        String fileName,
        Integer totalChunks,
        List<ChunkResponse> chunks
) {
}
