package com.acme.pdfingestion.dto;

public record UploadedDocumentResponse(
        String documentId,
        String fileName,
        Long fileSize,
        String status
) {
}
