package com.acme.aitraining.dto;

import java.util.List;

public record FastShowQueryResponse(
        String question,
        List<String> retrievedChunks,
        List<String> sources,
        String prompt,
        String answer
) {
}