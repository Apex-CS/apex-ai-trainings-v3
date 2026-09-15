package com.workshop.concierge.controller;

import java.util.List;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.workshop.concierge.dto.InventoryScanRequest;
import com.workshop.concierge.dto.InventoryScanResult;
import com.workshop.concierge.orchestration.SupervisorOrchestrator;

/**
 * Vision & Inventory Agent entry point.
 */
@RestController
public class InventoryController {

    private final SupervisorOrchestrator orchestrator;

    public InventoryController(SupervisorOrchestrator orchestrator) {
        this.orchestrator = orchestrator;
    }

    /**
     * Accepts a fridge/pantry photo (and/or a free-text item list) as multipart form data.
     */
    @PostMapping(value = "/api/v1/concierge/inventory/scan", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<InventoryScanResult> scanMultipart(
            @RequestParam(value = "image", required = false) MultipartFile image,
            @RequestParam(value = "textItems", required = false) List<String> textItems) {

        if (image != null && !image.isEmpty()) {
            return ResponseEntity.ok(orchestrator.scanInventoryFromImage(image));
        }
        if (textItems != null && !textItems.isEmpty()) {
            return ResponseEntity.ok(orchestrator.scanInventoryFromText(textItems));
        }
        throw new IllegalArgumentException("Either 'image' or 'textItems' must be provided");
    }

    /**
     * Accepts a raw JSON text item list instead of an image.
     */
    @PostMapping(value = "/api/v1/concierge/inventory/scan", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<InventoryScanResult> scanJson(@RequestBody InventoryScanRequest request) {
        if (request.textItems() == null || request.textItems().isEmpty()) {
            throw new IllegalArgumentException("textItems must not be empty");
        }
        return ResponseEntity.ok(orchestrator.scanInventoryFromText(request.textItems()));
    }
}
