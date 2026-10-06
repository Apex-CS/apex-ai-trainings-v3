package com.workshop.concierge.dto;

import java.util.List;

/**
 * A single recipe suggestion produced by the Recipe &amp; Grocery Agent.
 */
public record Recipe(
        String name,
        List<String> ingredients,
        List<String> steps,
        int calories,
        int proteinGrams,
        List<String> allergens
) {
}
