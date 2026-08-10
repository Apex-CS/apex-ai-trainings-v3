# Corporate Backend IT API

Demo Spring Boot service for IT operations: application server catalog, restarts, and restart history.

Runs on **port 8092** and connects to PostgreSQL database `owasp_it`.

Authentication uses the **same JWT tokens** as the financial API — see
[corporate-backend-financial-api/DEMO_TOKENS.md](../corporate-backend-financial-api/DEMO_TOKENS.md).

## Prerequisites

- Java 21
- Maven
- PostgreSQL from the repo `docker-compose` stack

## Database setup

If your Postgres container predates `owasp_it` in `docker/postgres/init.sql`:

```bash
docker exec owasp-postgres psql -U owasp -d owasp_ai -c "CREATE DATABASE owasp_it;"
```

## Run

```bash
cd corporate-backend-it-api
mvn spring-boot:run -Dspring-boot.run.workingDirectory=..
```

## API documentation

Swagger UI: [http://localhost:8092/swagger.html](http://localhost:8092/swagger.html)

## Endpoints

| Endpoint | Method | Roles |
|----------|--------|-------|
| `/api/list-app-servers` | GET | `it-admin`, `it-user` |
| `/api/restart-server` | POST | `it-admin` |
| `/api/list-app-restarts-by-app` | GET | `it-admin` |

### Registered application servers

| app_name | app_host              | owner_area |
|----------|-----------------------|------------|
| financial-backend | http://localhost:8091 | FINANCE |
| it-backend | http://localhost:8092 | IT |
| sales-backend | http://localhost:8093 | SALES |
| marketing-backend | http://localhost:8094 | MARKETING |

### Restart behavior

1. Inserts an `app_restarts` row with `operation_done = false`
2. Stops the process on the app port (`lsof` + `kill`)
3. Starts the Spring Boot app with `mvn spring-boot:run -Dspring-boot.run.workingDirectory=..`
4. Waits for `/actuator/health` to return UP
5. Updates `operation_done = true` on success

Restarting **it-backend** schedules a detached self-restart (the current process is stopped as part of that flow).

## Example requests

```bash
# List servers (it-user or it-admin)
curl -s "http://localhost:8092/api/list-app-servers" \
  -H "Authorization: Bearer <token>"

# Restart financial-backend (it-admin only — bart perez token)
curl -s -X POST "http://localhost:8092/api/restart-server" \
  -H "Authorization: Bearer <bart-token>" \
  -H "Content-Type: application/json" \
  -d '{"appName":"financial-backend"}'

# List restart history
curl -s "http://localhost:8092/api/list-app-restarts-by-app?appName=financial-backend" \
  -H "Authorization: Bearer <bart-token>"
```
