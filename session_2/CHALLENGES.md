# Hands-on Challenges

Pick ONE. Work solo or in pairs. Skeletons are in `main` — everything runs on local Ollama, no keys needed.

## Challenge A — Easy: "Commit Message Generator" (Spring AI)
Goal: repurpose the structured-output pattern.
1. In `spring-ai-demo`, create a record `CommitMessage(String type, String scope, String message)`.
2. Add a `POST /commit` endpoint that takes a diff (String body) and returns a `CommitMessage` using `.entity(...)`.
3. Test: `curl -X POST localhost:8080/commit --data-binary "renamed UserDao to UserRepository"`.
✔ Done when: you get valid typed JSON with type=refactor (or similar).

## Challenge B — Medium: chain two tools
Goal: understand the tool round trip.
1. Add a third `@Tool` to `OpsTools`: `openIncident(String service, String summary)` that returns a fake incident ID.
2. Ask `/ops`: "notifications is failing — open an incident and tell me who is on call."
3. Watch the DEBUG logs: how many model calls happened? Why?
✔ Done when: one answer uses ALL THREE tools and you can explain the sequence.

## Challenge C — Spicy: provider swap + quality comparison
Goal: experience "abstraction ≠ same behavior".
1. Run `/review` with the default (Ollama llama3.2) profile and save the JSON.
2. Run with `SPRING_PROFILES_ACTIVE=openai` (key provided by trainer) on the SAME input.
3. Compare: issue count, severity accuracy, hallucinated issues.
✔ Done when: you can name one concrete quality difference — discussion input for the wrap-up.

## Bonus — LangChain4j
Add a `@Tool` class to `langchain4j-demo` (`AiServices.builder(...).tools(new MyTools())`) and make the assistant use it.
