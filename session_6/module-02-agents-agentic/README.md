# Module 06 — Agents & Agentic AI: Kitchen Concierge

> **Objective:** Build a multi-agent Spring Boot application that turns a pantry/fridge photo or text list into a personalized meal plan using LangChain4j, Google Gemini, agent orchestration, guardrails, and memory.

- **Duration:** ~60–90 minutes
- **Difficulty:** Intermediate
- **Practice ID:** M06-P01

---

## Overview

This project demonstrates an agentic system that behaves more like a small AI operating team than a single LLM prompt. Instead of asking one model to do everything, the app splits the work into specialized agents:

- a vision + inventory agent that extracts ingredients from photos or text
- a nutritionist agent that evaluates dietary goals and restrictions
- a recipe + grocery agent that creates meal suggestions and missing-item lists
- a supervisor orchestrator that coordinates the process, applies safety checks, and keeps the flow structured

The result is a kitchen concierge that can take images, parse real pantry content, remember preferences for a user, and produce a guarded, personalized meal plan while avoiding allergens and redacting sensitive details.

---

## What This Project Teaches

- How to model a multi-agent workflow in Java with LangChain4j
- How to use `@AiService` interfaces for specialist agents
- How short-term memory and long-term memory differ in an LLM application
- How to add safety checks via guardrails instead of trusting model output blindly
- How a supervisor orchestrates multiple model calls instead of delegating everything to one monolithic prompt
- How to expose a real application with both REST endpoints and an embedded UI

---

## Architecture at a Glance

The system is organized around three major layers:

### 1. Agent Layer

The project defines several `@AiService` interfaces and turns them into model-backed agents via LangChain4j:

| Agent | Role | Key Responsibility |
|---|---|---|
| `VisionInventoryAgent` | Extraction | Parses pantry/fridge photos or free-text lists into structured inventory items |
| `NutritionistAgent` | Evaluation | Reads user goals and dietary preferences and produces a nutrition assessment |
| `RecipeGroceryAgent` | Planning | Generates recipes and missing ingredients while respecting allergies |
| `SupervisorOrchestrator` | Coordinator | Routes work, calls agents in sequence, and enforces policy checks |

The important idea is that each agent has a narrow responsibility and a clearer system prompt. That tends to produce more reliable results than one large “do everything” prompt.

### 2. Memory Layer

The app uses two memory models:

- **Short-term memory** via `MessageWindowChatMemory` tied to `sessionId`
- **Long-term memory** via an in-memory embedding store storing user preferences and allergies in vector form

This allows the app to remember a user’s preferences across sessions while still keeping each conversation bounded and context-aware.

### 3. Guardrail Layer

Safety is not optional in an agentic system. The app includes:

- `PiiSensitiveFilter` to remove card numbers, CVV codes, addresses, email addresses, and phone numbers from text before it reaches an LLM
- `AllergyGuardrail` to reject recipes containing user-declared allergens
- retry loops in the orchestrator that re-prompt the recipe agent until the plan passes validation or the retry limit is reached

This is a classic example of “LLM output must be checked before it is accepted.”

---

## Core Runtime Flow

### Inventory Scan Flow

1. The client calls `POST /api/v1/concierge/inventory/scan` with either:
   - an image multipart upload, or
   - a JSON body containing `textItems`
2. `InventoryController` routes the request to `SupervisorOrchestrator`
3. `SupervisorOrchestrator.scanInventoryFromImage(...)` or `scanInventoryFromText(...)` sends the input to `VisionInventoryAgent`
4. The agent produces a structured JSON result containing items, quantity, estimated expiration, and category
5. The result is sanitized and returned to the client as `InventoryScanResult`

If the direct LangChain4j vision route fails because of a transient Google AI capacity issue, the code falls back to a direct `GeminiVisionHttpClient` HTTP-based call. This is a practical production pattern: failover when a provider-backed tool is temporarily unavailable.

### Meal Plan Generation Flow

1. The client sends a `MealPlanRequest` with `userId`, `sessionId`, target macros, and the number of days
2. `SupervisorOrchestrator.generateMealPlan(...)` loads long-term preferences for that user
3. It extracts allergies from stored preferences and then asks `NutritionistAgent` to evaluate the meal strategy
4. For each day, it invokes `RecipeGroceryAgent` to generate recipes fitting the assessment and allergy rules
5. `AllergyGuardrail` validates every recipe against the user’s strict allergen list
6. If a recipe violates the policy, the orchestrator sends retry feedback and asks the agent to regenerate
7. Once a safe plan is accepted, the supervisor compiles the final `MealPlanResponse`
8. It enriches the output with grocery pricing/stock checks via `GroceryStoreTool`

This is the heart of the “agentic AI” pattern: multiple specialized steps, with validation and re-planning in the middle.

---

## Key Components

### 1. `SupervisorOrchestrator`

This class is the runtime brain of the application.

It is responsible for:

- receiving user requests from HTTP or UI
- loading memory and preferences
- orchestrating calls to the specialist agents
- applying guardrails after the model generates output
- logging every step to the console for observability

The orchestrator is intentionally not just a thin wrapper — it performs the policy and coordination logic that makes the system reliable.

### 2. `AiServiceConfig`

This configuration registers the specialized agents as Spring beans:

```java
@Bean
public VisionInventoryAgent visionInventoryAgent(ChatLanguageModel chatLanguageModel) {
    return AiServices.create(VisionInventoryAgent.class, chatLanguageModel);
}
```

The `AiServices` builder is the LangChain4j mechanism that binds a Java interface to an LLM-backed service with memory and tools.

### 3. `ChatMemoryConfig`

This sets up a sliding window memory that keeps a bounded conversation history for a given session:

```java
return sessionId -> MessageWindowChatMemory.builder()
        .id(sessionId)
        .maxMessages(windowSize)
        .build();
```

This is useful for short-term conversational context, such as before/after a sequence of meal-plan decisions within the same session.

### 4. `LongTermMemoryService`

This service uses an embedding store to persist user preference text:

- each preference is stored as a `TextSegment`
- metadata includes the `userId`
- retrieval is semantic, not simple keyword lookup

In other words, the app is not just storing raw strings; it is indexing them into embeddings and querying them by meaning.

### 5. `PiiSensitiveFilter`

Because the app can ingest handwritten receipt text or uploaded images with labels like “CVV” or addresses, the project redacts sensitive data before sending input to the model.

This is a practical and important safeguard in real-world AI systems.

### 6. `AllergyGuardrail`

The guardrail compares the generated recipe against the user’s allergy list and rejects it if it contains a forbidden ingredient or an allergen label.

The orchestrator then does a corrective loop:

- generate recipe
- validate it
- if invalid, re-prompt with retry feedback
- repeat until safe or max retries hit

This is a key demonstration of agentic control flow rather than “one-shot generation.”

---

## Security and Safety Patterns

This project is a good demonstration of practical AI safety patterns:

### PII redaction

The filter catches patterns like:

- card numbers
- CVV values
- street addresses
- email addresses
- phone numbers

This protects user data from leaking into prompts or logs.

### Allergy enforcement

The app assumes strict dietary restrictions matter. If a recipe includes a forbidden allergy in its name, ingredient list, or declared allergen metadata, it is rejected.

### Controlled retries

The system does not loop forever. It enforces a maximum retry count via `concierge.guardrails.max-recipe-retries` to avoid uncontrolled model churn.

---

## Long-Term Memory and Semantic Retrieval

This project demonstrates a very common “AI memory” pattern:

1. Save user preferences as embeddings
2. Query the embedding store with a generalized prompt such as “user dietary preferences, allergies and goals”
3. Retrieve semantically similar records for the current user

This is different from simple key-value storage. It lets the app find preferences even if the wording is not exact.

A demo user is seeded on startup:

- allergic to peanuts
- allergic to shellfish
- prefers high-protein, low-carb meals

This makes the app immediately testable without any manual setup.

---

## Tools and Tool Calling

The recipe agent is configured to use mock tools:

- `GroceryStoreTool.checkItemPriceAndStock(String item)`
- `RecipeDatabaseTool.findRecipesByIngredients(List<String> ingredients)`

These tools are registered through LangChain4j `AiServices.builder(...).tools(...)` patterns so the model can invoke them autonomously as part of tool-calling workflows.

In other words, the LLM is not just generating text. It is interacting with structured tool functions that return real values used in the orchestration flow.

---

## UI Layer

The app includes a lightweight embedded UI built with Javelit.

It starts automatically from `JavelitUiRunner` and is available at:

- `http://localhost:8501`

The interface contains three tabs:

1. **Inventory Scan** — upload an image or paste a list of groceries
2. **Meal Plan** — define `userId`, `sessionId`, calories, protein, and number of days
3. **Long-Term Preferences** — inspect or enrich the memory store

This is a nice demo because it shows the same orchestrator logic working in both API and UI mode without reimplementing the agent logic in two places.

---

## API Endpoints

### Inventory

```http
POST /api/v1/concierge/inventory/scan
```

Accepts either:

- multipart form data with `image` and/or `textItems`
- JSON body with `textItems`

Example JSON:

```json
{
  "textItems": [
    "eggs",
    "spinach",
    "chicken breast",
    "almond milk"
  ]
}
```

### Meal Plan

```http
POST /api/v1/concierge/meal-plan/generate
```

Example request:

```json
{
  "userId": "demo-user",
  "sessionId": "demo-session",
  "targetMacros": {
    "calories": 2000,
    "protein": 150
  },
  "days": 3
}
```

### Memory

```http
GET /api/v1/concierge/memory/preferences/{userId}
```

Returns the semantic preference list associated with that user.

---

## Configuration

The main configuration lives in `src/main/resources/application.yml`.

```yaml
server:
  port: 8086

concierge:
  gemini:
    api-key: ${GEMINI_API_KEY:}
    chat-model: ${GEMINI_CHAT_MODEL:gemini-3.5-flash}
    fallback-chat-model: ${GEMINI_FALLBACK_CHAT_MODEL:gemini-3.6-flash}
    second-fallback-chat-model: ${GEMINI_SECOND_FALLBACK_CHAT_MODEL:gemini-3.7-flash}
    embedding-model: ${GEMINI_EMBEDDING_MODEL:gemini-embedding-001}
    vision-model: ${GEMINI_VISION_MODEL:gemini-3.5-flash}
    timeout-seconds: ${GEMINI_TIMEOUT_SECONDS:120}
    max-retries: ${GEMINI_MAX_RETRIES:3}
  chat-memory:
    window-size: 10
  guardrails:
    max-recipe-retries: 2
  ui:
    port: ${UI_PORT:8501}
```

### Required environment variable

```bash
export GEMINI_API_KEY=your_api_key_here
```

You may also override the model names or UI port as needed.

---

## Step-by-Step to Run the Project

### 1. Build the app

```bash
cd session_6/module-02-agents-agentic
mvn clean package -DskipTests
```

### 2. Start the REST server

```bash
mvn spring-boot:run
```

Then open:

- REST API: `http://localhost:8086`
- UI: `http://localhost:8501`

### 3. Test the inventory endpoint

Upload a photo or send a text list such as:

```json
{
  "textItems": ["eggs", "spinach", "Greek yogurt", "chicken breast", "tomatoes"]
}
```

### 4. Generate a meal plan

Use the UI or call the endpoint with a sample request.

### 5. Inspect memory

Call:

```bash
curl http://localhost:8086/api/v1/concierge/memory/preferences/demo-user
```

---

## Example Project Flow

A realistic end-to-end scenario looks like this:

1. User uploads a fridge photo containing eggs, rice, broccoli, yogurt, and chicken
2. Vision agent extracts the inventory as structured items
3. The app loads the user’s stored preferences and allergies
4. Nutritionist agent evaluates the inventory against high-protein, low-carb goals
5. Recipe agent generates candidate recipes for three days
6. Allergy guardrail rejects any disallowed ingredients
7. The orchestrator retries with improved instructions until all recipes are safe
8. Grocery tools add a missing-item list with price and availability estimates
9. Final JSON is returned to the UI and/or API client

---

## Project Structure

```text
src/main/java/com/workshop/concierge/
├── agent/
│   ├── VisionInventoryAgent.java
│   ├── NutritionistAgent.java
│   ├── RecipeGroceryAgent.java
│   └── GeminiVisionHttpClient.java
├── config/
│   ├── AiServiceConfig.java
│   ├── ChatMemoryConfig.java
│   ├── ConciergeProperties.java
│   ├── FailoverChatLanguageModel.java
│   └── LangChain4jConfig.java
├── controller/
│   ├── InventoryController.java
│   ├── MealPlanController.java
│   └── MemoryController.java
├── dto/
│   ├── InventoryScanResult.java
│   ├── MealPlanRequest.java
│   ├── MealPlanResponse.java
│   └── ...
├── guardrail/
│   ├── AllergyGuardrail.java
│   ├── AllergyViolationException.java
│   └── PiiSensitiveFilter.java
├── memory/
│   ├── DemoPreferenceSeeder.java
│   └── LongTermMemoryService.java
├── orchestration/
│   └── SupervisorOrchestrator.java
├── tool/
│   ├── GroceryStoreTool.java
│   └── RecipeDatabaseTool.java
├── ui/
│   └── JavelitUiRunner.java
└── ConciergeApplication.java
```

---

## Troubleshooting

| Problem | Likely Cause | Solution |
|---|---|---|
| App fails at startup | Missing `GEMINI_API_KEY` | Export the API key before starting the app |
| Meal plan contains forbidden ingredients | Allergy rule not triggered or prompt drifted | Check stored preferences and review the allergy guardrail output |
| Vision call fails intermittently | Google AI service transient issue | The app includes a fallback direct Gemini HTTP request |
| UI does not open | Port conflict on `8501` | Set `UI_PORT` to another free port |
| Memory returns empty list | Preferences were never stored or embeddings are not initialized | Check the startup seeder and ensure the API key is valid |

---

## Extension Challenges

1. Add a dedicated `shopping-list` tool that groups missing ingredients by aisle category.
2. Add a `user preferences update` endpoint so preferences can be stored via API, not only via startup seeding.
3. Replace the in-memory embedding store with a real vector database such as Postgres + pgvector.
4. Add stronger validation around timestamps, meal calorie ranges, and user-specific dietary preferences.
5. Connect the orchestration flow to a more realistic external inventory system or grocery API.

---

## Key Takeaways

- **Agentic systems are workflows, not just prompts.** The orchestrator is the center of the design.
- **Specialized agents are more reliable than one large agent.** Narrow responsibilities lead to better output quality.
- **Memory matters.** Short-term memory handles dialogue flow, while long-term memory handles personalization.
- **Guardrails are essential.** Safety logic must validate the model’s output before it reaches users.
- **This app is a realistic demo of LLM orchestration in production Java.** It combines prompts, tools, memory, fallback logic, validation, and UI in one coherent system.

---

## Summary

This project is a complete example of an agentic AI workflow implemented in Java. It takes raw user input, converts it into structured data, remembers user constraints, generates a plan, validates it with guardrails, and delivers a clean output through both API and UI layers.

The end result is not just a demo app — it is a practical blueprint for building safer, more modular AI-powered systems in a real Spring Boot environment.
