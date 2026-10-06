package com.workshop.concierge.guardrail;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.workshop.concierge.dto.Recipe;

/**
 * Output Guardrail — validates a generated recipe against the user's strict allergy list.
 * This is applied by the {@code SupervisorOrchestrator} after every recipe generation call;
 * a violation triggers an automatic re-prompt of the Recipe & Grocery Agent.
 */
@Component
public class AllergyGuardrail {

    private static final Logger log = LoggerFactory.getLogger(AllergyGuardrail.class);

    /**
     * @return empty if the recipe is safe, otherwise a human-readable violation reason.
     */
    public Optional<String> validate(Recipe recipe, List<String> allergies) {
        if (allergies == null || allergies.isEmpty()) {
            return Optional.empty();
        }

        String haystack = String.join(" ", recipe.name(), String.join(" ", recipe.ingredients()))
                .toLowerCase(Locale.ROOT);

        for (String allergen : allergies) {
            String needle = allergen.toLowerCase(Locale.ROOT).trim();
            boolean declaredAllergen = recipe.allergens() != null && recipe.allergens().stream()
                    .anyMatch(a -> a.toLowerCase(Locale.ROOT).contains(needle));
            boolean mentionedInIngredients = !needle.isBlank() && haystack.contains(needle);

            if (declaredAllergen || mentionedInIngredients) {
                String reason = "Recipe '%s' violates allergy guardrail: contains '%s'"
                        .formatted(recipe.name(), allergen);
                log.warn("[AllergyGuardrail] {}", reason);
                return Optional.of(reason);
            }
        }
        return Optional.empty();
    }
}
