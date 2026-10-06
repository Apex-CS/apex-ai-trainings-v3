package com.workshop.concierge.dto;

/**
 * A single ingredient/item detected by the Vision &amp; Inventory Agent.
 */
public record InventoryItem(
        String name,
        String quantity,
        String estimatedExpiration,
        String category
) {
}
