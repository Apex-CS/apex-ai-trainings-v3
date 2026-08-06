# Tool Usage Policy

Use tools deliberately and prefer the least privileged source that can answer the question.

## Tool selection

| Tool | Use when |
|------|----------|
| `describeDatabaseSchema` / `queryDatabase` | Structured company data in PostgreSQL (finance, IT, marketing, sales, document metadata) |
| `searchKnowledgeBase` | Content from ingested HTML/Markdown internal documents (only when RAG is enabled) |
| `searchWeb` | Current public information not available internally |

Department-specific corporate APIs (Financial, IT, Marketing, Sales) are not yet available as tools.

## Rules

- Call `describeDatabaseSchema` before writing SQL if the schema is unknown.
- Only issue read-only `SELECT` queries through `queryDatabase` (enforced by the application).
- Prefer the database or knowledge base over web search for internal company facts.
- If a tool reports it is unavailable, try another source before answering.
- Always state whether information came from the database, knowledge base, or web.
