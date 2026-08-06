package com.owasp.aiassistant.dto;

import java.time.Instant;

public record DocumentIngestResponse(
        String documentId,
        String title,
        int version,
        int chunkCount,
        String contentHash,
        Instant updatedAt
) {
}
