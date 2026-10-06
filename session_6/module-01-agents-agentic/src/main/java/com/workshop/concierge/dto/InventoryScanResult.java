package com.workshop.concierge.dto;

import java.util.List;

/**
 * Structured output produced by the Vision &amp; Inventory Agent.
 */
public record InventoryScanResult(
        List<InventoryItem> items,
        String notes
) {
}
