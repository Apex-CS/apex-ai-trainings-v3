package com.owasp.aiassistant.dto;

import java.util.List;

public record ChatResponse(
        String answer,
        String conversationId,
        List<String> warnings
) {
}
