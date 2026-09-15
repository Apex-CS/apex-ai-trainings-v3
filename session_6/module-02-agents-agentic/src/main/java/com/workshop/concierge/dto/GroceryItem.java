package com.workshop.concierge.dto;

/**
 * A missing item to purchase, enriched with mock price/stock data from {@code GroceryStoreTool}.
 */
public record GroceryItem(
        String name,
        String quantity,
        boolean inStock,
        Double price
) {
}
