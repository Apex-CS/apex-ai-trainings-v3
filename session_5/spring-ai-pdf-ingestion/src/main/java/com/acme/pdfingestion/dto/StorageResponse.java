package com.acme.pdfingestion.dto;

public record StorageResponse(
        String collectionName,
        Integer chunksStored,
        Integer totalEmbeddingsStored,
        String status
) {
}
