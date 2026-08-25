package com.acme.pdfingestion.dto;

public record StoreRequestResponse(
        String fileName,
        String collectionName,
        Integer chunksStored,
        String status
) {
}
