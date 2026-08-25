package com.acme.pdfingestion.dto;

import java.util.List;
import java.util.Map;

public record ChromaRecordDetailsResponse(
        String id,
        String text,
        Integer dimensions,
        List<Float> embeddingPreview,
        Map<String, Object> metadata
) {
}
