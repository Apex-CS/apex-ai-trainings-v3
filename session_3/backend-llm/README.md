# Example Company AI Assistant

Spring Boot 21 application with **LangGraph4j** (ReAct agent), **Databricks** (chat), optional **Qdrant RAG**, **PostgreSQL**, and **MLflow**.

## Modes

| Mode | Profile | Requirements |
|------|---------|--------------|
| **Chat only** (default) | — | Databricks chat endpoint |

By default, chat calls `POST {DATABRICKS_HOST}/serving-endpoints/chat/completions` with `LLM_ENDPOINT_NAME` as the `model` field in the JSON body (OpenAI-compatible external/foundation model API).
| **Chat + RAG** | `rag` | Chat + embedding endpoint + Qdrant |

## Environment variables (chat only — default)

```bash
export DATABRICKS_HOST='https://your-workspace.cloud.databricks.com'
export DATABRICKS_TOKEN='dapi...'
export LLM_ENDPOINT_NAME='your-chat-endpoint'

# Optional URL override (legacy /invocations or route-optimized endpoints)
export DATABRICKS_CHAT_INVOCATION_URL='https://your-workspace.cloud.databricks.com/serving-endpoints/your-chat-endpoint/invocations'
```

## MLflow tracing

Chat turns are logged to MLflow (runs, metrics, and traces). Switch backends with `MLFLOW_TRACKING_URI`:

| Backend | `MLFLOW_TRACKING_URI` | Other variables |
|---------|----------------------|-----------------|
| **Local** (default) | unset, or `http://localhost:5001` | `docker compose up -d mlflow` |
| **Databricks** | `databricks` | `DATABRICKS_HOST`, `DATABRICKS_TOKEN`, `MLFLOW_EXPERIMENT_NAME` |

```bash
# Local (default) — uses docker-compose MLflow on port 5001
export MLFLOW_EXPERIMENT_NAME='owasp-chat'

# Databricks hosted tracking
export MLFLOW_TRACKING_URI='databricks'
export MLFLOW_EXPERIMENT_NAME='/Users/your_user_id/your-experiment'
```

Set `MLFLOW_AUTO_CREATE_EXPERIMENT=false` on Databricks if you prefer to create the experiment in the workspace UI first.

Traces include:
- Root `chat_agent` span with user message, assistant answer, and graph state
- Child spans per LangGraph node (`model`, `action_dispatcher`, and each tool node)
- Tool call arguments and message history on each step span
- Graph state fields such as `code_to_review` (currently initialized to `print('hello')`)

Each chat run and trace is tagged with:
- `user name` — demo user display name (e.g. `Bart Perez`)
- `user roles` — comma-separated roles (e.g. `it-admin,financial-user,marketing-user,sales-user`)
- `soft policy violations` / `hard policy violations` — violation counts for the turn

## Run (chat only)

```bash
docker compose up -d postgres   # SQL tool + document metadata (Qdrant not required)

cd backend-llm
./mvnw spring-boot:run
```

## Enable RAG later

When you have an embedding endpoint and Qdrant running:

```bash
docker compose up -d postgres qdrant

export EMBEDDING_ENDPOINT_NAME='databricks-gte-large-en'
export SPRING_PROFILES_ACTIVE=rag

./mvnw spring-boot:run
```

The `rag` profile enables Qdrant, Databricks embeddings, and document ingestion.

## Chat API

```bash
curl -X POST http://localhost:8080/api/chat \
  -H "Content-Type: application/json" \
  -d '{"message": "What company data is available in the database?"}'
```

## Agent tools

| Tool | Available (default) | Available (rag profile) |
|------|---------------------|------------------------|
| `searchWeb` | Yes | Yes |
| `queryDatabase` | Yes | Yes |
| `searchKnowledgeBase` | Returns "not enabled" | Yes |
| `getBudgetByArea` | Yes (Financial API on :8091) | Yes |
| `updateBudgetByArea` | Yes (Financial API on :8091) | Yes |
| `listAppServers` | Yes (IT API on :8092) | Yes |
| `restartAppServer` | Yes (IT API on :8092) | Yes |
| `listAppRestartsByApp` | Yes (IT API on :8092) | Yes |
| `getProducts` | Yes (Sales API on :8093) | Yes |
| `getSales` | Yes (Sales API on :8093) | Yes |
| Document ingest API | 503 / error | Yes |

Corporate API tools use demo JWTs from `app.corporate-api.demo-tokens` in `application.yml`.
Pass optional `demoUser` in the chat request (`FULANO_SMITH`, `SUTANO_DOE`, `MENGANA_DAVIDSON`, `BART_PEREZ`);
defaults to `BART_PEREZ`.

`getSales` returns redacted customer PII for users without `sales-admin`. Attempts to unredact masked data register a **hard policy violation**.

Submitting application credentials, tokens, or secrets (in chat or code attachments) — including `property=value` / `property: value` assignments and JWTs — is also a **hard policy violation**.
