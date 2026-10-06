# Session 6 - Agents and Agentic AI for Java Developers

This guide is a practical introduction to building agents in Java. It focuses on the smallest useful set of concepts and implementation patterns needed to build an agentic application with Spring Boot and LangChain4j.

The companion application is the Kitchen Concierge in [`module-02-agents-agentic`](module-02-agents-agentic/README.md). It demonstrates specialist agents, a supervisor workflow, tool calling, PII filtering, output guardrails, short-term chat memory, and long-term semantic memory.

## Learning goals

After reading this guide, you should be able to:

- explain what an agent is and when an ordinary Java method is better
- define an agent as a narrow Java interface with system and user instructions
- connect an agent to a chat model with `AiServices`
- distinguish deterministic workflows from model-driven decisions
- protect prompts and logs with PII filtering
- validate model output with guardrails before returning it
- use short-term memory for conversation context
- use long-term memory for durable user facts and preferences
- design a supervisor that coordinates multiple agents without giving one model unlimited control
- test the deterministic parts of an agentic system without depending on a live LLM

## 1. What is an agent?

An agent is a software component that uses a language model to interpret input, produce a decision or structured result, and possibly call approved tools. The model supplies probabilistic language understanding; the Java application supplies the contract, state, tools, policies, and execution boundaries.

A useful definition is:

> An agent is a model-backed capability with a clear responsibility, controlled inputs, typed outputs, and bounded authority.

An agent is not automatically an autonomous system. A prompt sent to a model is not a complete production agent unless the surrounding application also defines:

1. **Purpose** - what the agent is responsible for.
2. **Inputs** - which values it may use.
3. **Output contract** - what shape the result must have.
4. **Tools** - which functions it may call.
5. **Memory** - which context it may retain or retrieve.
6. **Policies** - what it must never do.
7. **Failure behavior** - timeout, retry, fallback, or rejection rules.
8. **Observability** - what can be logged safely and measured.

### Agent versus ordinary Java service

Use a normal Java service when the rule is fully known and deterministic:

```java
public boolean isEligible(Order order) {
    return order.total().compareTo(BigDecimal.valueOf(100)) >= 0
            && order.customerTier() == CustomerTier.GOLD;
}
```

Use an agent when the input requires language understanding, interpretation, extraction from unstructured content, or a bounded choice among several valid options:

```java
public interface SupportAgent {

    @SystemMessage("""
            You are a support triage agent. Classify the request, identify urgency,
            and return only the fields in the SupportTriage record.
            Never promise a refund or disclose internal policy.
            """)
    SupportTriage triage(@UserMessage String customerMessage);
}
```

The strongest designs combine both. Let the model interpret language, then let deterministic Java code enforce business rules and perform side effects.

## 2. The minimal agent specification

Before writing a prompt, describe the agent in a small specification. This prevents a vague "do everything" agent from becoming the architecture.

```text
Name: RecipeGroceryAgent
Purpose: Create a meal plan for one day from a nutrition assessment and inventory.
Inputs: session ID, day number, assessment, inventory, allergy list, retry feedback.
Output: structured daily meal plan.
Allowed tools: recipe lookup and grocery availability lookup.
Forbidden actions: ignore allergies, invent inventory, make medical claims.
Memory: short-term session context only; preferences come from a separate service.
Validation: JSON deserialization and allergy guardrail.
Failure policy: retry with feedback, then return only safe results with a warning.
Owner: SupervisorOrchestrator.
```

A minimal functional agent should have:

- one narrow responsibility
- a stable Java method signature
- explicit system instructions
- explicit user input variables
- a typed result where possible
- a timeout and retry limit
- a caller that validates the result

Do not start with autonomous planning, unrestricted tools, and shared memory. Add those capabilities only when a simpler service cannot solve the problem.

## 3. Defining an agent with LangChain4j

This project uses LangChain4j `AiServices` to turn Java interfaces into model-backed services. The interfaces in [`module-02-agents-agentic/src/main/java/com/workshop/concierge/agent`](module-02-agents-agentic/src/main/java/com/workshop/concierge/agent) are the best concrete examples.

### A small typed agent

```java
public record SentimentResult(String label, String reason) {}

public interface SentimentAgent {

    @SystemMessage("""
            You classify customer sentiment.
            Return a concise result with label POSITIVE, NEUTRAL, or NEGATIVE.
            Do not infer private facts that are not present in the message.
            """)
    @UserMessage("Classify this message: {{message}}")
    SentimentResult classify(@V("message") String message);
}
```

The annotations have clear roles:

- `@SystemMessage` defines the agent's stable role and constraints.
- `@UserMessage` defines the task-specific request.
- `@V("name")` binds a method parameter to a template variable.
- `@MemoryId` identifies the conversation whose short-term memory should be used.

The Kitchen Concierge uses this pattern in [`NutritionistAgent.java`](module-02-agents-agentic/src/main/java/com/workshop/concierge/agent/NutritionistAgent.java), [`RecipeGroceryAgent.java`](module-02-agents-agentic/src/main/java/com/workshop/concierge/agent/RecipeGroceryAgent.java), and [`VisionInventoryAgent.java`](module-02-agents-agentic/src/main/java/com/workshop/concierge/agent/VisionInventoryAgent.java).

### Registering the agent

A stateless agent can be created with `AiServices.create`:

```java
@Bean
public VisionInventoryAgent visionInventoryAgent(ChatLanguageModel model) {
    return AiServices.create(VisionInventoryAgent.class, model);
}
```

An agent that needs conversation memory uses the builder:

```java
@Bean
public NutritionistAgent nutritionistAgent(
        ChatLanguageModel model,
        ChatMemoryProvider memoryProvider) {
    return AiServices.builder(NutritionistAgent.class)
            .chatLanguageModel(model)
            .chatMemoryProvider(memoryProvider)
            .build();
}
```

The repository wires these agents in [`AiServiceConfig.java`](module-02-agents-agentic/src/main/java/com/workshop/concierge/config/AiServiceConfig.java).

### Output contracts

Prefer records or DTOs over unstructured strings when the result is part of an application workflow:

```java
public record InventoryItem(
        String name,
        String quantity,
        String estimatedExpiration,
        String category) {}
```

Typed output makes invalid data visible at a boundary. It does not prove that the model's values are correct, so semantic validation is still required afterward.

For a string result, parse and validate immediately. Do not pass raw model text through several services and hope that a later layer can interpret it.

## 4. Deterministic agents and deterministic control flow

"Deterministic agent" usually means one of two things:

1. A normal Java component whose behavior is fully determined by code and input.
2. An LLM agent placed inside a deterministic workflow with fixed steps, constrained tools, validation, and bounded retries.

The first kind is preferable for policy, authorization, calculations, persistence, and side effects. The second is useful when a model is needed for interpretation but the application must retain control.

### Deterministic workflow pattern

```java
public MealPlanResponse generateMealPlan(MealPlanRequest request) {
    List<String> preferences = memory.getPreferences(request.userId());
    NutritionAssessment assessment = nutritionist.evaluateNutrition(
            request.sessionId(), request.macros(), preferences, inventory);

    List<DayPlan> days = new ArrayList<>();
    for (int day = 1; day <= request.days(); day++) {
        DayPlan safePlan = generateAndValidateDay(request, assessment, day);
        days.add(safePlan);
    }

    return responseAssembler.create(request, days);
}
```

The Java code fixes the order of operations. The model fills in language-heavy decisions inside each step. This is easier to test, observe, retry, and explain than asking a single agent to invent the entire workflow.

### When to use a deterministic implementation

Use Java code for:

- authorization and access control
- PII handling
- allergy, compliance, and safety checks
- arithmetic and thresholds
- database writes
- retries, timeouts, and circuit breakers
- tool allowlists
- final response assembly

Use the model for:

- classifying a natural-language request
- extracting fields from text or images
- summarizing documents
- proposing candidates
- translating a human goal into structured intermediate data

The supervisor in [`SupervisorOrchestrator.java`](module-02-agents-agentic/src/main/java/com/workshop/concierge/orchestration/SupervisorOrchestrator.java) is an example of deterministic orchestration around model calls.

## 5. PII and sensitive data protection

PII must be handled before it reaches a model, an embedding store, or an application log. Prompt instructions such as "do not use the credit card number" are not a data protection strategy: the sensitive value has already crossed the boundary.

### Recommended data path

```text
raw request -> PII redaction -> validation -> agent/model -> output validation -> response
```

Apply the filter as close as possible to ingestion. Also consider:

- image OCR and vision inputs, not only plain text
- request logging, tracing, and exception messages
- chat memory and long-term memory
- model provider retention and regional processing
- access control for stored user preferences
- whether a value should be rejected instead of redacted

The example [`PiiSensitiveFilter.java`](module-02-agents-agentic/src/main/java/com/workshop/concierge/guardrail/PiiSensitiveFilter.java) redacts card numbers, CVV/CVC values, addresses, email addresses, and phone numbers. The orchestrator applies it before sending text to the inventory agent and before returning inventory notes.

```java
String safeInput = piiSensitiveFilter.redact(rawInput);
InventoryScanResult result = inventoryAgent.analyzeText(safeInput);
```

Redaction is not perfect identity protection. Regular expressions can miss formats and can produce false positives. For a real system, combine pattern matching with structured data classification, provider controls, access policies, and tests using representative sensitive inputs.

### PII checklist

- Define which data is prohibited, restricted, or allowed.
- Redact or reject before model invocation.
- Do not store raw input in chat or vector memory.
- Redact logs and telemetry.
- Test different separators, casing, country formats, and OCR errors.
- Keep replacement tokens stable, such as `[REDACTED-EMAIL]`.
- Treat uploaded images as sensitive even when their text is not yet extracted.

## 6. Guardrails: never trust model output by default

A guardrail is a deterministic check around a model call. Guardrails can run before the call, after the call, or both.

### Input guardrails

Input checks should reject or normalize:

- missing required values
- oversized prompts or uploads
- unsupported file types
- unsafe or forbidden requests
- PII and secrets
- invalid tenant or user identifiers

### Output guardrails

Output checks should validate:

- syntax and deserialization
- required fields and ranges
- authorization and policy constraints
- prohibited content or allergens
- whether tool results are consistent with the request
- whether a side effect is allowed before execution

The [`AllergyGuardrail.java`](module-02-agents-agentic/src/main/java/com/workshop/concierge/guardrail/AllergyGuardrail.java) checks recipe names, ingredients, and declared allergens. It returns a violation instead of silently accepting unsafe output.

```java
Optional<String> violation = allergyGuardrail.validate(recipe, allergies);
if (violation.isPresent()) {
    // Do not return or execute this recipe.
    retryFeedback = violation.get();
}
```

### Guardrail and retry pattern

```text
1. Call agent.
2. Parse the result.
3. Validate the result deterministically.
4. If valid, accept it.
5. If invalid and retries remain, provide bounded feedback and retry.
6. If retries are exhausted, fail closed or return a partial safe result.
```

Retries are not a substitute for validation. They are a recovery mechanism. Always cap them to control cost and latency. The example uses `concierge.guardrails.max-recipe-retries` and returns only safe recipes after the limit is reached.

For high-impact actions, use human approval or a separate authorization step. A guardrail that merely checks generated text is not enough to authorize payments, account changes, or destructive operations.

## 7. Short-term memory

Short-term memory is conversation context for one session. It helps an agent resolve references such as "use the second option" or maintain the current task across several turns.

It should be:

- bounded by a message or token limit
- scoped by a session or conversation ID
- isolated between users and tenants
- disposable or governed by a retention policy
- monitored for prompt growth and sensitive data

The example uses `MessageWindowChatMemory`:

```java
@Bean
public ChatMemoryProvider chatMemoryProvider(ConciergeProperties properties) {
    int windowSize = properties.getChatMemory().getWindowSize();
    return sessionId -> MessageWindowChatMemory.builder()
            .id(sessionId)
            .maxMessages(windowSize)
            .build();
}
```

See [`ChatMemoryConfig.java`](module-02-agents-agentic/src/main/java/com/workshop/concierge/config/ChatMemoryConfig.java). The `sessionId` is passed through an agent method annotated with `@MemoryId`.

Short-term memory is not a database of facts. A sliding window can evict an important preference, and retaining every message can expose sensitive information. Store durable facts separately and deliberately.

## 8. Long-term memory

Long-term memory stores facts that should survive a conversation, such as user preferences, constraints, or approved profile information. In a retrieval-augmented design, the application stores text with metadata and embeddings, then retrieves relevant records for a later request.

The example flow is:

```text
preference text -> TextSegment + userId metadata -> embedding -> EmbeddingStore
current request -> retrieval query embedding + userId filter -> relevant preferences
```

The implementation is in [`LongTermMemoryService.java`](module-02-agents-agentic/src/main/java/com/workshop/concierge/memory/LongTermMemoryService.java).

```java
public void savePreference(String userId, String preferenceText) {
    Metadata metadata = Metadata.from("userId", userId);
    TextSegment segment = TextSegment.from(preferenceText, metadata);
    Embedding embedding = embeddingModel.embed(segment).content();
    embeddingStore.add(embedding, segment);
}
```

Retrieval must always include a tenant or user filter. Similarity alone is not an authorization boundary. Also consider:

- update and deletion semantics
- duplicate preferences and stale facts
- source and timestamp metadata
- minimum similarity thresholds
- encryption and access control
- retention and user deletion requests
- whether the retrieved text is still appropriate for the current task

### Short-term versus long-term memory

| Concern | Short-term memory | Long-term memory |
|---|---|---|
| Scope | Current conversation | User or tenant over time |
| Storage | Chat messages | Facts, documents, embeddings, or database rows |
| Retrieval | Recent context | Explicit or semantic search |
| Lifetime | Session or bounded window | Persistent until updated or deleted |
| Main risk | Context growth and leakage | Stale data and cross-user retrieval |
| Example | "Use the recipe from earlier" | "User avoids peanuts" |

A robust application often uses both: short-term memory for dialogue and long-term memory for selected, governed facts.

## 9. Tools and authority boundaries

A tool is a normal application function exposed to an agent. Tool calls should be treated as requests, not permissions.

Good tools are:

- narrow and typed
- idempotent where possible
- observable
- protected by authorization
- limited in data returned
- explicit about errors

Examples in the demo include grocery price checks and recipe lookup. A production tool that sends an email, changes a record, or charges a card should require deterministic authorization and often human confirmation.

```java
public GroceryQuote checkItemPriceAndStock(String item) {
    // Query a controlled data source and return a typed result.
}
```

Do not expose a generic `executeSql`, unrestricted HTTP client, filesystem access, or a tool that accepts arbitrary code. The model should never decide its own authority boundary.

## 10. Multi-agent architecture

A multi-agent system is useful when responsibilities have different prompts, tools, memory, or validation rules. A common shape is:

```text
API/controller
    |
    v
supervisor (deterministic workflow)
    +--> extraction agent
    +--> evaluation agent
    +--> planning agent
    +--> output guardrails
    +--> deterministic tools
```

In the Kitchen Concierge:

- `VisionInventoryAgent` extracts inventory.
- `NutritionistAgent` evaluates goals and restrictions.
- `RecipeGroceryAgent` proposes daily meals.
- `SupervisorOrchestrator` controls sequencing, retries, memory retrieval, and final assembly.

Use separate agents when the separation makes behavior easier to understand and test. Do not create many agents just to rename prompt fragments. Each additional model call adds latency, cost, and another failure boundary.

## 11. Testing strategy for Java agents

Tests should separate deterministic application behavior from nondeterministic model quality.

### Unit test deterministic components

Test these without a model:

- PII patterns and redaction behavior
- guardrail acceptance and rejection
- DTO parsing and validation
- retry limits
- memory metadata filters
- authorization and tool policies
- orchestration branches

```java
@Test
void redactsPaymentDetailsBeforeModelInvocation() {
    String result = filter.redact("Card 4111 1111 1111 1111, CVV: 123");

    assertThat(result).contains("[REDACTED-CARD-NUMBER]");
    assertThat(result).contains("[REDACTED-CVV]");
    assertThat(result).doesNotContain("4111");
}
```

### Test model boundaries with fakes

Define a fake `NutritionistAgent` or `RecipeGroceryAgent` that returns known DTOs. Then test that the supervisor:

- invokes agents in the expected order
- rejects unsafe output
- retries only the failed step
- stops after the configured maximum
- returns a useful warning or error

### Use integration and evaluation tests separately

Integration tests verify wiring, serialization, provider configuration, and tool integration. Evaluation tests measure model behavior over a representative dataset. Do not make every unit test depend on a live provider, because availability, latency, and model versions make those tests fragile.

## 12. Production checklist

Before calling an agent ready for production, verify:

- The agent has one clear responsibility.
- Inputs and outputs are typed and validated.
- PII is filtered before model, memory, and logging boundaries.
- Every tool has a narrow allowlist and authorization check.
- Model output is validated before it is used.
- Timeouts, retries, fallback behavior, and maximum cost are defined.
- Memory is scoped, bounded, deletable, and protected from cross-user retrieval.
- Prompts and model versions are versioned.
- Logs contain correlation IDs but no secrets or raw sensitive prompts.
- Metrics cover latency, token usage, retries, validation failures, and tool errors.
- Failure is fail-closed for safety-sensitive operations.
- Tests cover policy behavior without requiring a live model.

## 13. Run the example

The complete walkthrough and API examples are in [`module-02-agents-agentic/README.md`](module-02-agents-agentic/README.md).

From the module directory:

```bash
cd session_6/module-02-agents-agentic
mvn test
mvn spring-boot:run
```

The demo expects a `GEMINI_API_KEY` as described in the module README. The main application runs on port `8086`, and the embedded UI uses port `8501` by default.

## Final mental model

For Java developers, the most important design rule is simple:

> Use the model for interpretation and proposals; use Java for authority, policy, state, and side effects.

An effective agent is therefore not a magical autonomous class. It is a carefully bounded Java component around a model call, embedded in a deterministic workflow, protected by PII controls and guardrails, and supported by memory only where memory is genuinely needed.
