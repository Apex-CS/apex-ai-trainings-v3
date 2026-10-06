package com.workshop.concierge.dto;

import java.util.List;

/**
 * One day of a multi-day meal plan.
 */
public record DayPlan(
        int day,
        List<Recipe> meals
) {
}
