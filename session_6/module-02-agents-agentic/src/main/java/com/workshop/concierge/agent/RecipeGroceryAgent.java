package com.workshop.concierge.agent;

import dev.langchain4j.service.MemoryId;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

/**
 * Recipe &amp; Grocery Agent — generates recipe options for a single day and a missing-items
 * shopping list, given the nutrition assessment and available inventory. May call
 * {@code GroceryStoreTool} and {@code RecipeDatabaseTool}.
 */
public interface RecipeGroceryAgent {

    @SystemMessage("""
            You are the Recipe & Grocery Agent of the Kitchen Concierge system.
            You generate recipe suggestions for one day of meals, using the available inventory as
            much as possible, respecting the nutrition assessment and STRICTLY avoiding any allergen
            listed in the user's allergies. For each recipe include: name, ingredients, steps,
            approximate calories, approximate protein grams, and a list of allergens present (if any,
            otherwise an empty list). List any ingredients required by the recipes that are not in
            the current inventory as missingIngredients. Grocery price and stock enrichment is
            handled by the supervisor after recipe generation. Return only valid JSON with this
            shape: {"recipes":[{"name":"...","ingredients":[],"steps":[],"calories":0,
            "proteinGrams":0,"allergens":[]}],"missingIngredients":[]}.
            """)
    @UserMessage("""
            Day: {{day}} of {{totalDays}}
            Nutrition assessment: {{nutritionAssessment}}
            Available inventory: {{inventory}}
            Strict allergies (must NEVER appear in any recipe): {{allergies}}
            {{retryFeedback}}
            """)
        String generateDailyPlan(@MemoryId String sessionId,
                                     @V("day") int day,
                                     @V("totalDays") int totalDays,
                                     @V("nutritionAssessment") String nutritionAssessment,
                                     @V("inventory") String inventory,
                                     @V("allergies") String allergies,
                                     @V("retryFeedback") String retryFeedback);
}
