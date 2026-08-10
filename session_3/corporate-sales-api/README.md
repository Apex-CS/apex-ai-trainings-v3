# Corporate Backend Sales API

Demo Spring Boot service for the Example Company rubber duck sales catalog and customer sales records (demo PII).

Runs on **port 8093** and connects to PostgreSQL database `owasp_sales`.

Authentication uses the **same JWT tokens** as the financial API — see
[corporate-backend-financial-api/DEMO_TOKENS.md](../corporate-backend-financial-api/DEMO_TOKENS.md).

## Prerequisites

- Java 21
- Maven
- PostgreSQL from the repo `docker-compose` stack

## Database setup

If your Postgres container predates `owasp_sales` in `docker/postgres/init.sql`:

```bash
docker exec owasp-postgres psql -U owasp -d owasp_ai -c "CREATE DATABASE owasp_sales;"
```

## Run

```bash
cd corporate-backend-sales-api
mvn spring-boot:run -Dspring-boot.run.workingDirectory=..
```

## API documentation

Swagger UI: [http://localhost:8093/swagger.html](http://localhost:8093/swagger.html)

## Endpoints

| Endpoint | Method | Roles |
|----------|--------|-------|
| `/api/get-products` | GET | `sales-admin`, `sales-user` |
| `/api/get-sales` | GET | `sales-admin`, `sales-user` |

### Products

Catalog aligned with the [Sales Division](backend-llm/src/main/resources/html_files_rag/company_sales_area.html) themes:

| Code | Product |
|------|---------|
| `CLASSIC_YELLOW` | Classic Yellow Rubber Duck |
| `GLOW_DUCKLING` | Glow Duckling Night Light |
| `CORP_EVENT_DUCK` | Corporate Custom Event Duck |
| `GLOBAL_DISTRO_KIT` | Global Distribution Starter Kit |
| `RETAIL_PARTNER_PACK` | Retail Partner Display Pack |
| `POOL_PARTY_BUNDLE` | Pool Party Celebration Bundle |
| `COLLECTOR_GOLDEN` | Collector Series Golden Duck |
| `CUSTOMER_SUCCESS_KIT` | Customer Success Welcome Kit |

### Sales data

72 randomly generated sales records include **demo customer PII** (name and phone). Phone numbers use fictional `+1-555-` exchanges with extra suffix digits so they do not correspond to real numbers.

**Role-based PII access:**

| Role | Customer name / phone |
|------|------------------------|
| `sales-admin` | Full PII |
| `sales-user` | Redacted with asterisks (`*`); transaction details remain visible |

Requests to unredact masked customer PII through the assistant are treated as **hard policy violations** for non-admin users.

### Example requests

```bash
# List products (sutano doe = sales-admin)
curl -s "http://localhost:8093/api/get-products" \
  -H "Authorization: Bearer <sutano-token>"

# List all sales with customer PII
curl -s "http://localhost:8093/api/get-sales" \
  -H "Authorization: Bearer <sutano-token>"

# Filter by product
curl -s "http://localhost:8093/api/get-sales?product=CLASSIC_YELLOW" \
  -H "Authorization: Bearer <sutano-token>"
```
