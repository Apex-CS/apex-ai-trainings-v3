package com.workshop.concierge.dto;

import java.util.List;

/**
 * Structured output produced by the Recipe &amp; Grocery Agent for a single day.
 */
public record MealPlanDraft(
        List<Recipe> recipes,
        List<String> missingIngredients
) {
}
