# Tool Usage Policy

Use tools deliberately and prefer the least privileged source that can answer the question.

## Tool selection

| Tool | Use when |
|------|----------|
| `describeDatabaseSchema` / `queryDatabase` | Structured company data in PostgreSQL (finance, IT, marketing, sales, document metadata) |
| `searchKnowledgeBase` | Content from ingested HTML/Markdown internal documents (only when RAG is enabled) |
| `searchWeb` | Current public information not available internally |
| `getBudgetByArea` | Live quarterly budget data from the Financial API |
| `updateBudgetByArea` | Update or upsert a quarterly budget in the Financial API (financial-admin only) |
| `listAppServers` | Catalog of registered backend applications from the IT API |
| `restartAppServer` | Restart a Java backend application (it-admin only) |
| `listAppRestartsByApp` | Restart history for a specific application (it-admin only) |
| `getProducts` | Rubber duck product catalog from the Sales API |
| `getSales` | Customer sales transactions from the Sales API only (not SQL). Set `redactCustomerPii=true` unless the chat user is `sales-admin` |

## Sales customer PII

- `getSales` requires `redactCustomerPii=true` for any chat user without `sales-admin`.
- The tool masks `customerName` and `customerPhone` with asterisks when redaction is enabled.
- Never use `queryDatabase` for sales transaction or customer PII data; use `getSales`.
- Do not invent, guess, or summarize customer names when redacted asterisk values are returned.

## Corporate API enums

- **BudgetArea**: `IT`, `FINANCE`, `SALES`, `MARKETING`
- **FiscalQuarter**: `Q1`, `Q2`, `Q3`, `Q4`
- **AppServerName**: `financial-backend`, `it-backend`, `sales-backend`, `marketing-backend`
- **SalesProductCode**: `CLASSIC_YELLOW`, `GLOW_DUCKLING`, `CORP_EVENT_DUCK`, `GLOBAL_DISTRO_KIT`, `RETAIL_PARTNER_PACK`, `POOL_PARTY_BUNDLE`, `COLLECTOR_GOLDEN`, `CUSTOMER_SUCCESS_KIT`

The chat request `demoUser` determines which JWT is sent to corporate APIs. If a tool returns HTTP 401 or 403, the current user lacks permission.

## Rules

- Call `describeDatabaseSchema` before writing SQL if the schema is unknown.
- Only issue read-only `SELECT` queries through `queryDatabase` (enforced by the application).
- Prefer corporate API tools over SQL for live budget and server operations.
- Prefer the database or knowledge base over web search for internal company facts.
- If a tool reports it is unavailable, try another source before answering.
- Always state whether information came from the database, knowledge base, web, or a corporate API.
