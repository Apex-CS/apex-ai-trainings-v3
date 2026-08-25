package com.acme.pdfingestion.dto;

public record ChromaCollectionSummaryResponse(
        String collectionName,
        Long recordCount
) {
}
