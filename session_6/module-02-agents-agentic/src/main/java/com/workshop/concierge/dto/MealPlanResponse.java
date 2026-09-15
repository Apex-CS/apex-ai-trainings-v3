package com.workshop.concierge.dto;

import java.util.List;

public record MealPlanResponse(
        String userId,
        String sessionId,
        List<DayPlan> mealPlan,
        List<GroceryItem> groceryList,
        List<String> warnings
) {
}
