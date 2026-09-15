package com.workshop.concierge.dto;

import java.util.List;

public record UserPreferencesResponse(
        String userId,
        List<String> preferences
) {
}
