package com.workshop.concierge.orchestration;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.workshop.concierge.agent.GeminiVisionHttpClient;
import com.workshop.concierge.agent.NutritionistAgent;
import com.workshop.concierge.agent.RecipeGroceryAgent;
import com.workshop.concierge.agent.VisionInventoryAgent;
import com.workshop.concierge.config.ConciergeProperties;
import com.workshop.concierge.dto.DayPlan;
import com.workshop.concierge.dto.GroceryItem;
import com.workshop.concierge.dto.InventoryScanResult;
import com.workshop.concierge.dto.MealPlanDraft;
import com.workshop.concierge.dto.MealPlanRequest;
import com.workshop.concierge.dto.MealPlanResponse;
import com.workshop.concierge.dto.NutritionAssessment;
import com.workshop.concierge.dto.Recipe;
import com.workshop.concierge.guardrail.AllergyGuardrail;
import com.workshop.concierge.guardrail.PiiSensitiveFilter;
import com.workshop.concierge.memory.LongTermMemoryService;
import com.workshop.concierge.tool.GroceryStoreTool;

import dev.langchain4j.data.message.Content;
import dev.langchain4j.data.message.ImageContent;
import dev.langchain4j.data.message.TextContent;

/**
 * Supervisor Orchestrator Agent — routes incoming requests, coordinates the specialized
 * agents step by step, applies safety guardrails, and prints every decision/interaction to
 * the console for observability/demo purposes.
 */
@Service
public class SupervisorOrchestrator {

    private static final Logger log = LoggerFactory.getLogger(SupervisorOrchestrator.class);
    private static final Pattern ALLERGY_PATTERN = Pattern.compile("(?i)allerg\\w*\\s+to\\s+([a-z ,]+)");

    private final VisionInventoryAgent visionInventoryAgent;
    private final NutritionistAgent nutritionistAgent;
    private final RecipeGroceryAgent recipeGroceryAgent;
    private final AllergyGuardrail allergyGuardrail;
    private final PiiSensitiveFilter piiSensitiveFilter;
    private final LongTermMemoryService longTermMemoryService;
    private final GroceryStoreTool groceryStoreTool;
    private final ConciergeProperties properties;
    private final ObjectMapper objectMapper;
    private final GeminiVisionHttpClient geminiVisionHttpClient;

    public SupervisorOrchestrator(VisionInventoryAgent visionInventoryAgent,
                                   NutritionistAgent nutritionistAgent,
                                   RecipeGroceryAgent recipeGroceryAgent,
                                   AllergyGuardrail allergyGuardrail,
                                   PiiSensitiveFilter piiSensitiveFilter,
                                   LongTermMemoryService longTermMemoryService,
                                   GroceryStoreTool groceryStoreTool,
                                   ConciergeProperties properties,
                                   ObjectMapper objectMapper,
                                   GeminiVisionHttpClient geminiVisionHttpClient) {
        this.visionInventoryAgent = visionInventoryAgent;
        this.nutritionistAgent = nutritionistAgent;
        this.recipeGroceryAgent = recipeGroceryAgent;
        this.allergyGuardrail = allergyGuardrail;
        this.piiSensitiveFilter = piiSensitiveFilter;
        this.longTermMemoryService = longTermMemoryService;
        this.groceryStoreTool = groceryStoreTool;
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.geminiVisionHttpClient = geminiVisionHttpClient;
    }

    // ---------------------------------------------------------------- inventory scan

    public InventoryScanResult scanInventoryFromImage(MultipartFile image) {
        String mimeType = Optional.ofNullable(image.getContentType()).orElse("image/jpeg");
        return scanInventoryFromImageBytes(readBytes(image), mimeType);
    }

    public InventoryScanResult scanInventoryFromImageBytes(byte[] imageBytes, String mimeType) {
        console("Supervisor", "Routing inventory scan request -> Vision & Inventory Agent (image mode)");
        String base64 = Base64.getEncoder().encodeToString(imageBytes);
        ImageContent imageContent = ImageContent.from(base64, mimeType != null ? mimeType : "image/jpeg");

        String instructions = "Analyze this pantry/fridge photo (or receipt) and extract the structured inventory. "
            + "If this looks like a receipt, ignore address/payment details entirely. "
            + "Return only valid JSON with this shape: {\"items\":[{\"name\":\"...\","
            + "\"quantity\":\"...\",\"estimatedExpiration\":\"...\",\"category\":\"...\"}],"
            + "\"notes\":\"...\"}.";
        List<Content> contents = List.of(TextContent.from(instructions), imageContent);
        String rawResult;
        try {
            rawResult = visionInventoryAgent.analyzeImage(contents);
        } catch (RuntimeException exception) {
            if (!isTransientFailure(exception)) {
                throw exception;
            }
            console("Supervisor", "LangChain4j vision path is capacity-limited; using direct Gemini fallback");
            rawResult = geminiVisionHttpClient.analyze(imageBytes, mimeType, instructions);
        }
        InventoryScanResult result = parseImageScanResult(rawResult);
        console("VisionInventoryAgent", "Extracted " + result.items().size() + " item(s) from image");
        return sanitize(result);
    }

    private InventoryScanResult parseImageScanResult(String rawResult) {
        String json = rawResult == null ? "" : rawResult.trim();
        if (json.startsWith("```")) {
            json = json.replaceFirst("^```(?:json)?\\s*", "").replaceFirst("\\s*```$", "");
        }
        try {
            return objectMapper.readValue(json, InventoryScanResult.class);
        } catch (JsonProcessingException exception) {
            throw new IllegalArgumentException("Vision agent returned invalid inventory JSON", exception);
        }
    }

    private boolean isTransientFailure(Throwable exception) {
        Throwable current = exception;
        while (current != null) {
            String message = current.getMessage();
            if (message != null && (message.contains("503") || message.contains("UNAVAILABLE")
                    || message.toLowerCase(Locale.ROOT).contains("high demand")
                    || message.toLowerCase(Locale.ROOT).contains("timeout"))) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }

    public InventoryScanResult scanInventoryFromText(List<String> textItems) {
        console("Supervisor", "Routing inventory scan request -> Vision & Inventory Agent (text mode)");
        String sanitizedInput = piiSensitiveFilter.redact(String.join(", ", textItems));
        InventoryScanResult result = visionInventoryAgent.analyzeText(sanitizedInput);
        console("VisionInventoryAgent", "Extracted " + result.items().size() + " item(s) from text list");
        return sanitize(result);
    }

    private InventoryScanResult sanitize(InventoryScanResult result) {
        String safeNotes = piiSensitiveFilter.redact(result.notes());
        if (safeNotes != null && !safeNotes.equals(result.notes())) {
            console("PiiSensitiveFilter", "Redacted sensitive content from inventory notes");
        }
        return new InventoryScanResult(result.items(), safeNotes);
    }

    private byte[] readBytes(MultipartFile image) {
        try {
            return image.getBytes();
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to read uploaded image", e);
        }
    }

    // ---------------------------------------------------------------- meal plan generation

    public MealPlanResponse generateMealPlan(MealPlanRequest request) {
        console("Supervisor", "Routing meal-plan request for userId=%s sessionId=%s (days=%d)"
                .formatted(request.userId(), request.sessionId(), request.days()));

        List<String> longTermPreferences = longTermMemoryService.getPreferences(request.userId());
        console("Supervisor", "Loaded " + longTermPreferences.size() + " long-term preference(s) for userId=" + request.userId());

        List<String> allergies = extractAllergies(longTermPreferences);
        console("Supervisor", "Derived strict allergy list: " + allergies);

        String macros = "calories=%d, proteinGrams=%d".formatted(
                request.targetMacros().calories(), request.targetMacros().protein());
        String preferencesText = String.join("; ", longTermPreferences);
        String inventoryText = "No prior inventory scan supplied for this session; "
                + "assume a general, moderately-stocked kitchen with common staples.";

        console("Supervisor", "Invoking Nutritionist Agent");
        NutritionAssessment assessment = nutritionistAgent.evaluateNutrition(
                request.sessionId(), macros, preferencesText, inventoryText);
        console("NutritionistAgent", "Assessment: " + assessment.summary());

        List<DayPlan> mealPlan = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        Set<String> missingIngredients = new LinkedHashSet<>();

        for (int day = 1; day <= request.days(); day++) {
            console("Supervisor", "Invoking Recipe & Grocery Agent for day " + day);
            DayPlan dayPlan = generateGuardedDailyPlan(request, assessment, inventoryText, allergies, day, warnings);
            mealPlan.add(dayPlan);
        }

        // Missing ingredients are recomputed from the final, guardrail-approved recipes'
        // ingredient lists (best-effort demo heuristic; a real system would track
        // inventory-vs-recipe deltas explicitly).
        mealPlan.forEach(dayPlan -> dayPlan.meals().forEach(recipe -> missingIngredients.addAll(recipe.ingredients())));

        console("Supervisor", "Enriching grocery list with GroceryStoreTool price/stock checks");
        List<GroceryItem> groceryList = missingIngredients.stream()
                .map(this::checkGroceryItem)
                .toList();

        console("Supervisor", "Meal plan generation complete. warnings=" + warnings.size());
        return new MealPlanResponse(request.userId(), request.sessionId(), mealPlan, groceryList, warnings);
    }

    private DayPlan generateGuardedDailyPlan(MealPlanRequest request,
                                              NutritionAssessment assessment,
                                              String inventoryText,
                                              List<String> allergies,
                                              int day,
                                              List<String> warnings) {
        String allergiesText = String.join(", ", allergies);
        String retryFeedback = "";
        int maxRetries = properties.getGuardrails().getMaxRecipeRetries();

        for (int attempt = 0; attempt <= maxRetries; attempt++) {
                String rawDraft = recipeGroceryAgent.generateDailyPlan(
                    request.sessionId(), day, request.days(),
                    assessment.summary(), inventoryText, allergiesText, retryFeedback);
                MealPlanDraft draft = parseMealPlanDraft(rawDraft);

            List<Recipe> safeRecipes = new ArrayList<>();
            List<String> violations = new ArrayList<>();

            for (Recipe recipe : draft.recipes()) {
                Optional<String> violation = allergyGuardrail.validate(recipe, allergies);
                if (violation.isPresent()) {
                    violations.add(violation.get());
                } else {
                    safeRecipes.add(recipe);
                }
            }

            if (violations.isEmpty()) {
                console("AllergyGuardrail", "Day " + day + " passed allergy validation (" + safeRecipes.size() + " recipe(s))");
                return new DayPlan(day, safeRecipes);
            }

            console("AllergyGuardrail", "Day " + day + " attempt " + (attempt + 1) + " rejected: " + violations);

            if (attempt == maxRetries) {
                warnings.add("Day %d: %d recipe(s) rejected after %d retries due to allergy guardrail; only safe recipes are included."
                        .formatted(day, violations.size(), maxRetries));
                console("Supervisor", "Max retries reached for day " + day + "; keeping only guardrail-safe recipes");
                return new DayPlan(day, safeRecipes);
            }

            retryFeedback = "IMPORTANT: your previous suggestion was REJECTED because it violated these allergies: "
                    + violations + ". Regenerate strictly avoiding all listed allergens.";
        }

        // Unreachable, loop always returns.
        return new DayPlan(day, List.of());
    }

    private GroceryItem checkGroceryItem(String itemName) {
        String toolResult = groceryStoreTool.checkItemPriceAndStock(itemName);
        return parseGroceryToolResult(itemName, toolResult);
    }

    private MealPlanDraft parseMealPlanDraft(String rawDraft) {
        String json = rawDraft == null ? "" : rawDraft.trim();
        if (json.startsWith("```")) {
            json = json.replaceFirst("^```(?:json)?\\s*", "").replaceFirst("\\s*```$", "");
        }
        try {
            return objectMapper.readValue(json, MealPlanDraft.class);
        } catch (JsonProcessingException exception) {
            throw new IllegalArgumentException("Recipe agent returned invalid meal-plan JSON", exception);
        }
    }

    private GroceryItem parseGroceryToolResult(String itemName, String toolResult) {
        double price = 0.0;
        boolean inStock = false;
        Matcher priceMatcher = Pattern.compile("price=\\$([0-9.]+)").matcher(toolResult);
        if (priceMatcher.find()) {
            price = Double.parseDouble(priceMatcher.group(1));
        }
        Matcher stockMatcher = Pattern.compile("inStock=(true|false)").matcher(toolResult);
        if (stockMatcher.find()) {
            inStock = Boolean.parseBoolean(stockMatcher.group(1));
        }
        return new GroceryItem(itemName, "1", inStock, price);
    }

    private List<String> extractAllergies(List<String> preferences) {
        List<String> allergies = new ArrayList<>();
        for (String preference : preferences) {
            Matcher matcher = ALLERGY_PATTERN.matcher(preference);
            if (matcher.find()) {
                for (String allergen : matcher.group(1).split(",")) {
                    String trimmed = allergen.trim().toLowerCase(Locale.ROOT);
                    if (!trimmed.isBlank()) {
                        allergies.add(trimmed);
                    }
                }
            }
        }
        return allergies;
    }

    private void console(String actor, String message) {
        String line = "[%s] %s".formatted(actor, message);
        System.out.println(line);
        log.info(line);
    }
}
