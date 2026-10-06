package com.workshop.concierge.dto;

/**
 * Structured output produced by the Nutritionist Agent, evaluating the requested
 * macros/dietary restrictions against the available inventory.
 */
public record NutritionAssessment(
        String summary,
        java.util.List<String> recommendedFoods,
        java.util.List<String> foodsToAvoid
) {
}
