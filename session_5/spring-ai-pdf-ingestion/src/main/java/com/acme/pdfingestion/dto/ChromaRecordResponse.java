package com.acme.pdfingestion.dto;

import java.util.List;
import java.util.Map;

public record ChromaRecordResponse(
        String id,
        String sourceDocument,
        Integer chunkNumber,
        Integer dimensions,
        String textPreview,
        List<Float> embeddingPreview,
        Map<String, Object> metadata
) {
}
