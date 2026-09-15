package com.workshop.concierge.agent;

import com.workshop.concierge.dto.NutritionAssessment;
import dev.langchain4j.service.MemoryId;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

/**
 * Nutritionist Agent — evaluates calorie/macro goals and dietary restrictions (allergies,
 * low-carb, etc.) stored in long-term memory against the currently available inventory.
 */
public interface NutritionistAgent {

    @SystemMessage("""
            You are the Nutritionist Agent of the Kitchen Concierge system.
            Given a user's target macros, their long-term dietary preferences/allergies, and their
            current pantry/fridge inventory, produce a concise nutrition assessment: what should be
            prioritized to hit the targets, and what foods must be avoided due to allergies or
            restrictions. NEVER recommend a food that appears in the user's allergy list.
            """)
    @UserMessage("""
            Target macros: {{macros}}
            Long-term preferences/allergies: {{preferences}}
            Available inventory: {{inventory}}

            Produce a nutrition assessment (summary, recommendedFoods, foodsToAvoid).
            """)
    NutritionAssessment evaluateNutrition(@MemoryId String sessionId,
                                           @V("macros") String macros,
                                           @V("preferences") String preferences,
                                           @V("inventory") String inventory);
}
