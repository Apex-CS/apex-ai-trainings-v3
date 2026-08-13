# Module 05 — End-to-End Lab: Enterprise Release Integration Agent

> **Objective:** Combine all previous modules into a complete release agent that checks Jira for critical bugs, conditionally triggers deployment, enforces OAuth2 authentication, and requires human approval for PROD.

- **Duration:** ~90 minutes
- **Difficulty:** Advanced
- **Practice ID:** M05-P01

---

## Overview

You are building an automated release agent for your enterprise. The agent **orchestrates two MCP Servers** — a Jira MCP Server (Module 02 mock) and a Secure Deployment MCP Server (Module 04) — applying the following business rules:

> "Only deploy if there are zero critical open bugs in Jira for the target release version. Always require human approval when deploying to PROD."

---

## Prerequisites

| Requirement | How to verify |
|---|---|
| All modules 01–04 completed | — |
| All Docker services running | `docker compose up -d` |
| Valid OAuth2 token from Keycloak | See Step 1 |

---

## Learning Outcomes

- Orchestrate multiple `McpSyncClient` instances in a single Java service
- Implement stateful multi-step agent logic: **check → gate → deploy**
- Chain OAuth2 Token Relay across two MCP Servers
- Handle `PENDING_APPROVAL` responses in agent orchestration
- Test the complete flow end-to-end with mock services
- Understand production deployment considerations

---

## Tech Stack

| Component | Details |
|---|---|
| Spring AI MCP Client | 1.0.0 — connects to Jira + Deployment MCP Servers |
| Spring Boot Web | REST endpoint: `POST /release` |
| Spring Security 6 | OAuth2 Resource Server — `/release` requires Bearer token |
| WireMock | Jira mock on port 9001, Deployment API mock on port 9003 |
| Keycloak | OAuth2 Authorization Server on port 8180 |
| Module 04 MCP Server | Secure Deployment MCP Server on port 8084 |

---

## Architecture

```
User  ──POST /release──►  ReleaseIntegrationAgent (port 8085)
                                │
           ┌────────────────────┴────────────────────┐
           │                                         │
           ▼ (SSE, public)                           ▼ (SSE, Bearer token relay)
  Jira MCP Server                        Secure Deployment MCP Server
  (WireMock port 9001)                   (Module 04, port 8084)
           │                                         │
    jira_search_issues                        triggerDeployment
           │                                         │
           ▼                                         ▼
  [2 critical bugs?]                       [DEV/STAGING → deploy]
       BLOCKED                             [PROD → PENDING_APPROVAL]
```

### Agent Flow

```
1. POST /release/PROJ/2.4/PROD  →  ReleaseIntegrationAgent.executeRelease()
2. Agent calls jira_search_issues  (project=PROJ, fixVersion=2.4)
3. If critical bugs found  →  return BLOCKED with bug list
4. If no critical bugs     →  call triggerDeployment(payment-service, v2.4, PROD)
5. If env=PROD             →  Deployment MCP Server returns PENDING_APPROVAL
6. Agent returns approvalUrl to caller
7. Human approves via POST /confirm/{requestId}
8. Caller retries  →  deployment executes
```

---

## Step-by-Step Instructions

### Step 1 — Start All Services and Verify Health

```bash
cd /root/projects/java_workshop
docker compose up -d

echo "=== Service Health Check ==="
curl -sf http://localhost:9001/__admin/health  && echo "WireMock Jira:       OK" || echo "WireMock Jira:       FAIL"
curl -sf http://localhost:9003/__admin/health  && echo "WireMock Deployment: OK" || echo "WireMock Deployment: FAIL"
curl -sf http://localhost:8180/realms/workshop/.well-known/openid-configuration \
  | jq .issuer | grep -q workshop && echo "Keycloak:            OK" || echo "Keycloak:            FAIL"
curl -sf http://localhost:8084/actuator/health \
  | jq .status | grep -q UP && echo "Secure Deployment MCP: OK" || echo "Secure Deployment MCP: FAIL"
```

**Expected:**
```
WireMock Jira:         OK
WireMock Deployment:   OK
Keycloak:              OK
Secure Deployment MCP: OK
```

---

### Step 2 — Review `ReleaseIntegrationAgent.java` — The Orchestrator

Open [ReleaseIntegrationAgent.java](src/main/java/com/workshop/mcp/module05/agent/ReleaseIntegrationAgent.java). This is the core business logic.

```java
@Service
public class ReleaseIntegrationAgent {

    public ReleaseResult executeRelease(
            String projectKey, String version,
            String applicationName, String environment,
            String bearerToken) {

        // ── Step 1: Jira critical bug check ──────────────────────────────────
        List<JiraIssueDTO> blockers = checkJiraForBlockers(projectKey, version);

        if (!blockers.isEmpty()) {
            return ReleaseResult.blocked(projectKey, version, blockers);
        }

        // ── Step 2: Trigger deployment via Secure Deployment MCP Server ──────
        return triggerDeployment(applicationName, version, environment, bearerToken);
    }

    private List<JiraIssueDTO> checkJiraForBlockers(String projectKey, String version) {
        // Jira MCP Server is public — no auth needed
        try (McpSyncClient jiraClient = buildPublicClient(jiraMcpUrl)) {
            jiraClient.initialize();
            String jql = "project=%s AND priority=Critical AND issuetype=Bug AND fixVersion=\"%s\" AND status!=Done"
                    .formatted(projectKey, version);
            var result = jiraClient.callTool(new McpSchema.CallToolRequest(
                    "jira_search_issues", Map.of("jql", jql, "maxResults", 50)));
            return deserializeList(extractText(result));
        }
    }

    private ReleaseResult triggerDeployment(
            String appName, String version, String environment, String bearerToken) {
        // Deployment MCP Server requires OAuth2 — Token Relay
        try (McpSyncClient deployClient = buildAuthenticatedClient(deploymentMcpUrl, bearerToken)) {
            deployClient.initialize();
            var result = deployClient.callTool(new McpSchema.CallToolRequest(
                    "triggerDeployment",
                    Map.of("applicationName", appName, "version", version, "environment", environment)));
            return parseResult(extractText(result));
        }
    }
}
```

**Token Relay:**
```java
private McpSyncClient buildAuthenticatedClient(String serverUrl, String bearerToken) {
    var transport = HttpClientSseClientTransport.builder(serverUrl)
            .sseEndpoint("/sse")
            // Forward caller's JWT to the downstream MCP Server
            .customizeRequest(builder -> builder
                    .header("Authorization", "Bearer " + bearerToken))
            .build();
    return McpClient.sync(transport)
            .clientInfo(new McpSchema.Implementation("release-agent", "1.0.0"))
            .build();
}
```

> **Key concept:** The user's `Bearer` token flows from the REST request → Release Agent → Deployment MCP Server → Deployment REST API. No re-authentication, no service accounts. The **user's identity** flows end-to-end.

---

### Step 3 — Build and Start the Release Agent

```bash
cd module-05-e2e-lab
mvn clean package -DskipTests
mvn spring-boot:run &

sleep 5
curl -s http://localhost:8085/actuator/health | jq .status
```

**Expected:** `"UP"`

---

### Step 4 — Obtain an OAuth2 Token

```bash
TOKEN=$(curl -s -X POST \
  http://localhost:8180/realms/workshop/protocol/openid-connect/token \
  -d 'grant_type=client_credentials&client_id=mcp-client&client_secret=mcp-secret' \
  | jq -r '.access_token')

echo "Token obtained (first 50 chars): ${TOKEN:0:50}..."
```

---

### Scenario 1 — Release 2.4 with Critical Bugs → BLOCKED

The WireMock Jira mock returns 2 critical open bugs for release 2.4.

```bash
curl -s -X POST http://localhost:8085/release \
  -H 'Content-Type: application/json' \
  -H "Authorization: Bearer $TOKEN" \
  -d '{
    "projectKey": "PROJ",
    "version": "2.4",
    "applicationName": "payment-service",
    "environment": "STAGING"
  }' | jq
```

**Expected response:**
```json
{
  "status": "BLOCKED",
  "version": "2.4",
  "message": "Resolve 2 critical bug(s) before deploying release 2.4",
  "blockers": [
    { "key": "PROJ-101", "summary": "NPE in payment processor when card token is null", "priority": "Critical" },
    { "key": "PROJ-108", "summary": "Deadlock in session management under high concurrency", "priority": "Critical" }
  ]
}
```

---

### Scenario 2 — Release 3.0 to DEV → DEPLOYED immediately

Release 3.0 has no critical bugs in Jira. DEV deployments need no human approval.

```bash
curl -s -X POST http://localhost:8085/release \
  -H 'Content-Type: application/json' \
  -H "Authorization: Bearer $TOKEN" \
  -d '{
    "projectKey": "PROJ",
    "version": "3.0",
    "applicationName": "payment-service",
    "environment": "DEV"
  }' | jq
```

**Expected response:**
```json
{
  "status": "DEPLOYED",
  "applicationName": "payment-service",
  "version": "3.0",
  "environment": "DEV",
  "message": "Successfully deployed payment-service 3.0 to DEV"
}
```

---

### Scenario 3 — Release 3.0 to PROD → Human Approval Required

This scenario exercises the complete human-in-the-loop flow.

**Step A — Attempt PROD deployment:**
```bash
RESPONSE=$(curl -s -X POST http://localhost:8085/release \
  -H 'Content-Type: application/json' \
  -H "Authorization: Bearer $TOKEN" \
  -d '{"projectKey":"PROJ","version":"3.0","applicationName":"payment-service","environment":"PROD"}')

echo $RESPONSE | jq
```

**Expected Step A response:**
```json
{
  "status": "PENDING_APPROVAL",
  "message": "PROD deployment requires human approval. Please visit the approval URL.",
  "approvalRequestId": "a1b2c3d4-...",
  "approvalUrl": "/confirm/a1b2c3d4-..."
}
```

**Step B — Extract the requestId:**
```bash
REQUEST_ID=$(echo $RESPONSE | jq -r '.approvalRequestId')
echo "Pending approval: $REQUEST_ID"
```

**Step C — Human approves the deployment:**
```bash
curl -s -X POST http://localhost:8084/confirm/$REQUEST_ID \
  -H "Authorization: Bearer $TOKEN" | jq
```

**Expected Step C response:**
```json
{
  "approved": true,
  "requestId": "a1b2c3d4-...",
  "approvedBy": "workshop-user",
  "message": "Deployment approved. It will execute on the next retry."
}
```

**Step D — Retry the release (now approved):**
```bash
curl -s -X POST http://localhost:8085/release \
  -H 'Content-Type: application/json' \
  -H "Authorization: Bearer $TOKEN" \
  -d '{"projectKey":"PROJ","version":"3.0","applicationName":"payment-service","environment":"PROD"}' | jq
```

**Expected Step D response:**
```json
{
  "status": "DEPLOYED",
  "applicationName": "payment-service",
  "version": "3.0",
  "environment": "PROD"
}
```

---

### Scenario 4 — No Token → 401

```bash
curl -s -w '\nHTTP: %{http_code}' \
  -X POST http://localhost:8085/release \
  -H 'Content-Type: application/json' \
  -d '{"projectKey":"PROJ","version":"3.0","applicationName":"payment-service","environment":"DEV"}'
```

**Expected:** `HTTP: 401`

---

### Step 5 — Review End-to-End Audit Trail

Inspect the complete audit log from the PROD deployment flow:

```bash
tail -50 /tmp/module04-audit.log | jq -sc '.' | jq '.[] | {
  eventType,
  toolName,
  callerEmail,
  extra: (del(.timestamp, .eventType, .toolName, .callerSub, .callerEmail, .callerUsername))
}'
```

**Expected event sequence for Scenario 3:**

| # | `eventType` | `toolName` | Notes |
|---|---|---|---|
| 1 | `TOOL_INVOKED` | `triggerDeployment` | First call — PROD environment |
| 2 | `APPROVAL_REQUIRED` | `triggerDeployment` | Human approval pending |
| 3 | `APPROVAL_GRANTED` | `n/a` | Human clicked confirm |
| 4 | `TOOL_INVOKED` | `triggerDeployment` | Retry after approval |
| 5 | `TOOL_COMPLETED` | `triggerDeployment` | `outcome: SUCCESS` |

---

### Step 6 — Production Readiness Checklist

Review what changes are needed to make this workshop solution production-ready:

| Area | Workshop Implementation | Production Recommendation |
|---|---|---|
| **MCP Client Connections** | New `McpSyncClient` per request | Pool authenticated SSE sessions per user identity; use `McpAsyncClient` |
| **Human-in-the-Loop Persistence** | `ConcurrentHashMap` — lost on restart | Redis or PostgreSQL with TTL; auto-expire stale approvals |
| **Token Relay** | Raw Bearer token in headers | Spring Security OAuth2 Token Relay filter; validate expiry before relay |
| **Rate Limiting** | Single JVM Resilience4j counter | Distributed via Redis Lua scripts or API Gateway (Kong, Azure APIM) |
| **Audit Log Routing** | Local file | Logstash/Fluentd → Elasticsearch/Splunk; enable log integrity signing |
| **MCP Server Discovery** | Hardcoded URLs in `application.yml` | Service mesh (Istio) or Kubernetes Service DNS |
| **Transport Security** | Plain HTTP | Mutual TLS (mTLS) between agent and MCP servers; cert rotation via Vault |

---

## Troubleshooting

| Problem | Cause | Solution |
|---|---|---|
| Release agent returns "Jira search failed" immediately | WireMock Jira mock not running | `docker compose up -d wiremock-jira` → verify: `curl http://localhost:9001/__admin/health` |
| Cannot connect to port 8084 | Module 04 MCP Server not running | `cd module-04-security && mvn spring-boot:run` |
| PROD deployment never becomes `DEPLOYED` after approval | Agent does not auto-retry | This is by design — caller must retry. In production, use a Kafka/SQS event to trigger auto-retry. |
| Token expired during multi-step scenario | Keycloak default 600s expiry | Re-run the token request: `TOKEN=$(curl -s ... \| jq -r '.access_token')` |

---

## Extension Challenges

1. **Async approval webhook:** When `/confirm` is called, publish a `DeploymentApproved` event to a Kafka topic that automatically triggers a retry — eliminating the manual retry step.
2. **Azure DevOps integration:** In addition to Jira, also check ADO work items before approving the release.
3. **Dry-run mode:** Add a `dryRun: true` parameter to simulate the full flow without calling any real APIs — useful for testing agent logic.
4. **Approval timeout:** Automatically reject `PENDING_APPROVAL` requests older than 4 hours via a scheduled task.

---

## Key Takeaways

- **Multi-MCP orchestration:** a single Java agent can chain calls to multiple specialized MCP Servers.
- **Token Relay end-to-end:** the user's identity flows from the REST request → agent → Jira MCP → Deployment MCP → Deployment API.
- **`PENDING_APPROVAL` is a first-class state** — agents must handle it gracefully, not treat it as an error.
- **Human-in-the-loop + structured audit logs** = the minimum viable governance framework for production LLM agents.
- **Production readiness** requires connection pooling, distributed rate limits, persistent approvals, and mTLS — the workshop builds the correct mental model; these are the next steps.
