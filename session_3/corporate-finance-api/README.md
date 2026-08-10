# Corporate Backend Financial API

Demo Spring Boot service for area budgets with JWT role-based access control.

Runs on **port 8091** and connects to PostgreSQL database `owasp_financial`.

## Prerequisites

- Java 21
- Maven
- PostgreSQL from the repo `docker-compose` stack (port from `.env` → `POSTGRES_HOST_PORT`, default `5433`)

## Database setup

Flyway creates the `budget` table and seeds 5 years of data (2024–2028) on startup.

If your Postgres container was created **before** `owasp_financial` was added to `docker/postgres/init.sql`, create the database once:

```bash
docker exec -it owasp-postgres psql -U owasp -d owasp_ai -c "CREATE DATABASE owasp_financial;"
```

New containers pick up the database automatically from `init.sql`.

## Run

Run from the repository root so Spring can load `.env` (as configured in `application.yml`):

```bash
cd corporate-backend-financial-api
mvn spring-boot:run -Dspring-boot.run.workingDirectory=..
```

## Authentication

JWT bearer tokens are documented in [DEMO_TOKENS.md](./DEMO_TOKENS.md).

| Endpoint | Roles |
|----------|-------|
| `GET /api/get-budget-by-area` | `financial-admin`, `financial-user` |
| `PUT /api/update-budget-by-area` | `financial-admin` |

## API

### GET `/api/get-budget-by-area`

Query parameters:

- `area` (required): `IT`, `FINANCE`, `SALES`, or `MARKETING`
- `fiscalYear` (optional): filter to a single fiscal year

### PUT `/api/update-budget-by-area`

Upserts a budget row for the given area, quarter, and year.

```json
{
  "area": "IT",
  "fiscalQuarter": 1,
  "fiscalYear": 2026,
  "budget": 375000.00
}
```

The authenticated user's `sub` claim is stored in `user_modified`.

## API documentation

Swagger UI is available at [http://localhost:8091/swagger.html](http://localhost:8091/swagger.html).

Use the **Authorize** button and paste a bearer token from [DEMO_TOKENS.md](./DEMO_TOKENS.md) to try the endpoints.

## Health

```bash
curl http://localhost:8091/actuator/health
```
