package com.acme.aitraining.dto;

import java.util.List;

public record FastShowUploadResponse(
        String collection,
        int filesUploaded,
        int chunksCreated,
        int embeddingsCreated,
        List<String> files,
        String status
) {
}