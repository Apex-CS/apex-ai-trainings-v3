package com.workshop.concierge.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import com.workshop.concierge.dto.UserPreferencesResponse;
import com.workshop.concierge.memory.LongTermMemoryService;

/**
 * Long-term memory (vector store) read access.
 */
@RestController
public class MemoryController {

    private final LongTermMemoryService longTermMemoryService;

    public MemoryController(LongTermMemoryService longTermMemoryService) {
        this.longTermMemoryService = longTermMemoryService;
    }

    @GetMapping("/api/v1/concierge/memory/preferences/{userId}")
    public ResponseEntity<UserPreferencesResponse> getPreferences(@PathVariable String userId) {
        var preferences = longTermMemoryService.getPreferences(userId);
        return ResponseEntity.ok(new UserPreferencesResponse(userId, preferences));
    }
}
