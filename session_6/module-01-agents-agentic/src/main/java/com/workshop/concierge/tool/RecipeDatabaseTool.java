package com.workshop.concierge.tool;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import dev.langchain4j.agent.tool.Tool;

/**
 * Mock recipe database integration exposed to the LLM as a {@code @Tool}.
 * In a real system this would query a recipe search service (e.g. Spoonacular, internal DB).
 */
@Component
public class RecipeDatabaseTool {

    private static final Logger log = LoggerFactory.getLogger(RecipeDatabaseTool.class);

    @Tool("Finds candidate recipe names that can be made from (or mostly from) the given list of " +
            "available ingredients. Returns a comma-separated list of recipe name suggestions.")
    public String findRecipesByIngredients(List<String> ingredients) {
        String suggestion = "Grilled %s bowl, %s stir-fry, %s salad".formatted(
                firstOrDefault(ingredients, "chicken"),
                firstOrDefault(ingredients, "vegetable"),
                firstOrDefault(ingredients, "greens"));
        log.info("[RecipeDatabaseTool] ingredients={} -> {}", ingredients, suggestion);
        return suggestion;
    }

    private String firstOrDefault(List<String> ingredients, String fallback) {
        if (ingredients == null || ingredients.isEmpty()) {
            return fallback;
        }
        return ingredients.get(0);
    }
}
