package com.workshop.concierge.dto;

import java.util.List;

/**
 * Alternate JSON payload for /inventory/scan when a raw text item list is supplied
 * instead of (or in addition to) an image.
 */
public record InventoryScanRequest(
        List<String> textItems
) {
}
