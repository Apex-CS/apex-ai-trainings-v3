package com.acme.pdfingestion.dto;

import java.util.Map;

public record ChromaRecordDetailResponse(
        String id,
        String text,
        Map<String, Object> metadata
) {
}
