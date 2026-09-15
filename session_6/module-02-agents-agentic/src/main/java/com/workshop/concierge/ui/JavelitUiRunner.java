package com.workshop.concierge.ui;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import com.workshop.concierge.config.ConciergeProperties;
import com.workshop.concierge.dto.DayPlan;
import com.workshop.concierge.dto.GroceryItem;
import com.workshop.concierge.dto.InventoryItem;
import com.workshop.concierge.dto.InventoryScanResult;
import com.workshop.concierge.dto.MealPlanRequest;
import com.workshop.concierge.dto.MealPlanResponse;
import com.workshop.concierge.dto.Recipe;
import com.workshop.concierge.dto.TargetMacros;
import com.workshop.concierge.memory.LongTermMemoryService;
import com.workshop.concierge.orchestration.SupervisorOrchestrator;

import io.javelit.core.Jt;
import io.javelit.core.JtContainer;
import io.javelit.core.JtUploadedFile;
import io.javelit.core.Server;

/**
 * Embeds a Javelit UI (a Streamlit-like Java data-app framework) alongside the REST API,
 * calling the same {@code SupervisorOrchestrator}/{@code LongTermMemoryService} beans directly
 * (no HTTP hop) so every UI action goes through the exact same agents/guardrails.
 */
@Component
public class JavelitUiRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(JavelitUiRunner.class);

    private final SupervisorOrchestrator orchestrator;
    private final LongTermMemoryService longTermMemoryService;
    private final ConciergeProperties properties;

    public JavelitUiRunner(SupervisorOrchestrator orchestrator,
                            LongTermMemoryService longTermMemoryService,
                            ConciergeProperties properties) {
        this.orchestrator = orchestrator;
        this.longTermMemoryService = longTermMemoryService;
        this.properties = properties;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        int port = properties.getUi().getPort();
        Server server = Server.builder(this::renderApp, port).build();
        server.start();
        log.info("[JavelitUI] Kitchen Concierge UI available at http://localhost:{}", port);
    }

    private void renderApp() throws Exception {
        Jt.title("🍽️ Kitchen Concierge").use();
        Jt.markdown("Multi-agent kitchen assistant — Vision & Inventory, Nutritionist, "
                + "Recipe & Grocery agents, orchestrated with allergy guardrails.").use();

        var tabs = Jt.tabs(List.of("Inventory Scan", "Meal Plan", "Long-Term Preferences")).use();
        renderInventoryTab(tabs.tab("Inventory Scan"));
        renderMealPlanTab(tabs.tab("Meal Plan"));
        renderPreferencesTab(tabs.tab("Long-Term Preferences"));
    }

    // ---------------------------------------------------------------- inventory scan tab

    private void renderInventoryTab(JtContainer container) {
        Jt.subheader("Scan a Pantry / Fridge Photo or Item List").use(container);

        List<JtUploadedFile> files = Jt.fileUploader("Upload a photo")
                .type(List.of(".png", ".jpg", ".jpeg"))
                .use(container);
        String textItems = Jt.textArea("...or paste a comma-separated item list")
                .placeholder("e.g. eggs, spinach, chicken breast")
                .use(container);

        if (Jt.button("Scan Inventory").key("scan-inventory-btn").use(container)) {
            try {
                InventoryScanResult result = runInventoryScan(files, textItems);
                Jt.sessionState().put("lastInventoryResult", result);
                if (files != null && !files.isEmpty()) {
                    Jt.sessionState().put("lastInventoryImage", files.get(0));
                }
            } catch (Exception e) {
                log.error("Inventory scan failed", e);
                Jt.sessionState().put("lastInventoryError", e.getMessage());
            }
        }

        Object error = Jt.sessionState().get("lastInventoryError");
        if (error != null) {
            Jt.error("Scan failed: " + error).use(container);
        }

        Object last = Jt.sessionState().get("lastInventoryResult");
        if (last instanceof InventoryScanResult result) {
            Object uploadedImage = Jt.sessionState().get("lastInventoryImage");
            if (uploadedImage instanceof JtUploadedFile image) {
                Jt.markdown("**Uploaded image**").use(container);
                Jt.image(image).use(container);
            }
            Jt.table(toInventoryRows(result.items())).use(container);
            if (result.notes() != null && !result.notes().isBlank()) {
                Jt.markdown("**Notes:** " + result.notes()).use(container);
            }
        }
    }

    private InventoryScanResult runInventoryScan(List<JtUploadedFile> files, String textItems) {
        if (files != null && !files.isEmpty()) {
            JtUploadedFile file = files.get(0);
            return orchestrator.scanInventoryFromImageBytes(file.content(), file.contentType());
        }
        if (textItems != null && !textItems.isBlank()) {
            List<String> items = List.of(textItems.split(","));
            return orchestrator.scanInventoryFromText(items);
        }
        throw new IllegalArgumentException("Upload an image or enter a text item list first");
    }

    private List<InventoryRow> toInventoryRows(List<InventoryItem> items) {
        List<InventoryRow> rows = new ArrayList<>();
        for (InventoryItem item : items) {
            rows.add(new InventoryRow(item.name(), item.quantity(), item.estimatedExpiration(), item.category()));
        }
        return rows;
    }

    // ---------------------------------------------------------------- meal plan tab

    private void renderMealPlanTab(JtContainer container) {
        Jt.subheader("Generate a Multi-Day Meal Plan").use(container);

        var cols = Jt.columns(2).use(container);
        String userId = Jt.textInput("User ID").value("demo-user").use(cols.col(0));
        String sessionId = Jt.textInput("Session ID").value("demo-session").use(cols.col(1));

        var macroCols = Jt.columns(3).use(container);
        Integer calories = Jt.numberInput("Target calories", Integer.class).value(2000).use(macroCols.col(0));
        Integer protein = Jt.numberInput("Target protein (g)", Integer.class).value(150).use(macroCols.col(1));
        Integer days = Jt.numberInput("Days", Integer.class).value(3).use(macroCols.col(2));

        if (Jt.button("Generate Meal Plan").key("generate-plan-btn").use(container)) {
            try {
                MealPlanRequest request = new MealPlanRequest(userId, sessionId,
                        new TargetMacros(calories, protein), days);
                MealPlanResponse response = orchestrator.generateMealPlan(request);
                Jt.sessionState().put("lastMealPlan", response);
                Jt.sessionState().put("lastMealPlanError", null);
            } catch (Exception e) {
                log.error("Meal plan generation failed", e);
                Jt.sessionState().put("lastMealPlanError", e.getMessage());
            }
        }

        Object error = Jt.sessionState().get("lastMealPlanError");
        if (error != null) {
            Jt.error("Meal plan generation failed: " + error).use(container);
        }

        Object last = Jt.sessionState().get("lastMealPlan");
        if (last instanceof MealPlanResponse response) {
            renderMealPlanResult(container, response);
        }
    }

    private void renderMealPlanResult(JtContainer container, MealPlanResponse response) {
        for (String warning : response.warnings()) {
            Jt.warning(warning).use(container);
        }

        List<RecipeRow> recipeRows = new ArrayList<>();
        for (DayPlan dayPlan : response.mealPlan()) {
            for (Recipe recipe : dayPlan.meals()) {
                recipeRows.add(new RecipeRow(dayPlan.day(), recipe.name(), recipe.calories(),
                        recipe.proteinGrams(), String.join(", ", recipe.ingredients()),
                        String.join(", ", recipe.allergens())));
            }
        }
        Jt.markdown("**Recipes**").use(container);
        Jt.table(recipeRows).use(container);

        List<GroceryRow> groceryRows = new ArrayList<>();
        for (GroceryItem item : response.groceryList()) {
            groceryRows.add(new GroceryRow(item.name(), item.quantity(), item.inStock(), item.price()));
        }
        Jt.markdown("**Grocery List (missing items)**").use(container);
        Jt.table(groceryRows).use(container);
    }

    // ---------------------------------------------------------------- preferences tab

    private void renderPreferencesTab(JtContainer container) {
        Jt.subheader("Long-Term Preferences & Allergies").use(container);

        String userId = Jt.textInput("User ID").value("demo-user").key("prefs-user-id").use(container);

        if (Jt.button("Load Preferences").key("load-prefs-btn").use(container)) {
            Jt.sessionState().put("lastPreferences", longTermMemoryService.getPreferences(userId));
        }

        Object last = Jt.sessionState().get("lastPreferences");
        if (last instanceof List<?> preferences) {
            List<PreferenceRow> rows = new ArrayList<>();
            for (Object preference : preferences) {
                rows.add(new PreferenceRow(String.valueOf(preference)));
            }
            Jt.table(rows).use(container);
        }

        Jt.markdown("---").use(container);
        String newPreference = Jt.textInput("Add a new preference/allergy")
                .placeholder("e.g. Allergic to peanuts")
                .key("new-preference-input")
                .use(container);
        if (Jt.button("Save Preference").key("save-preference-btn").use(container)
                && newPreference != null && !newPreference.isBlank()) {
            longTermMemoryService.savePreference(userId, newPreference);
            Jt.success("Saved preference for userId=" + userId).use(container);
        }
    }

    // ---------------------------------------------------------------- table row shapes

    public record InventoryRow(String name, String quantity, String expiration, String category) {
    }

    public record RecipeRow(int day, String name, int calories, int proteinGrams, String ingredients, String allergens) {
    }

    public record GroceryRow(String name, String quantity, boolean inStock, Double price) {
    }

    public record PreferenceRow(String preference) {
    }
}
