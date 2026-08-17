# Module 02 — Consuming External MCPs: Jira Integration

> **Objective:** Configure a Java MCP Client to connect to an external MCP Server (Jira mock), dynamically discover its tools, invoke them, and map JSON-RPC responses to typed Java DTOs.

- **Duration:** ~60 minutes
- **Difficulty:** Intermediate
- **Practice ID:** M02-P01

---

## Overview

A production Java service needs to query Jira for critical bugs before approving a release. Rather than hardcoding Jira's REST API, you'll connect to a **Jira MCP Server** (mocked with WireMock) using Spring AI's MCP Client over **SSE transport**. You'll discover tools at runtime, invoke them, and deserialize the JSON responses into typed Java DTOs.

---

## Prerequisites

| Requirement | How to verify |
|---|---|
| Module 01 completed | Understand JSON-RPC basics |
| Docker running | `docker ps` |
| WireMock Jira mock started | `docker compose up -d wiremock-jira` |

---

## Learning Outcomes

- Configure `McpSyncClient` with SSE transport in Spring Boot
- Discover available tools from a remote MCP Server at startup
- Invoke tools programmatically and parse responses
- Map JSON-RPC text content to typed Java DTOs using Jackson
- Handle MCP errors (`isError: true`) gracefully in client code
- Understand the difference between **stdio** and **SSE** transports

---

## Tech Stack

| Component | Details |
|---|---|
| Spring Boot | 3.3.5 (`spring-boot-starter-web`) |
| Spring AI MCP Client | 1.0.0 (`spring-ai-mcp-client-spring-boot-starter`) |
| Jackson Databind | 2.17.x |
| WireMock | 3.9.1 — mock Jira MCP Server |

---

## SSE Transport Architecture

SSE (Server-Sent Events) transport flow:

```
1. MCP Client  ──GET /sse──────────────────►  MCP Server  (opens persistent event stream)
2. MCP Server  ──event: endpoint──────────►  MCP Client  (sends POST URL)
3. MCP Client  ──POST /mcp/message (JSON-RPC request)──►  MCP Server
4. MCP Server  ──event: message (JSON-RPC response)───►  MCP Client
5. Connection persists for the lifetime of the client session
```

**stdio vs SSE:**
- **stdio** — ideal for local CLIs and single-agent use. Single client only.
- **SSE** — works across network boundaries, through load balancers, supports multiple concurrent clients. Required for microservices.

---

## Step-by-Step Instructions

### Step 1 — Start the Jira MCP Mock Server

WireMock simulates a Jira MCP Server that speaks the MCP SSE protocol. The mock responds to the MCP handshake and Jira-specific tool calls.

```bash
cd /root/projects/apex-ai-trainings-v3/session_4
docker compose up -d wiremock-jira

# Verify it's up
curl -s http://localhost:9001/__admin/mappings | jq '.mappings | length'
```

**Expected output:** `7`

> **Tip:** Inspect all stubs: `curl http://localhost:9001/__admin/mappings | jq`

---

### Step 2 — Review WireMock Stubs — How the Mock MCP Server Works

Examine [wiremock/mappings/jira-mcp-stubs.json](wiremock/mappings/jira-mcp-stubs.json). These stubs implement the full MCP SSE handshake protocol.

The `tools/list` stub returns two Jira tools:

```json
{
  "request": {
    "method": "POST",
    "url": "/mcp/message",
    "bodyPatterns": [{ "matchesJsonPath": "$[?(@.method == 'tools/list')]" }]
  },
  "response": {
    "status": 200,
    "jsonBody": {
      "jsonrpc": "2.0",
      "id": "{{jsonPath request.body '$.id'}}",
      "result": {
        "tools": [
          {
            "name": "jira_get_issue",
            "description": "Retrieves a Jira issue by its key (e.g. PROJ-123)",
            "inputSchema": {
              "type": "object",
              "properties": {
                "issueKey": { "type": "string", "description": "Jira issue key, e.g. PROJ-123" }
              },
              "required": ["issueKey"]
            }
          },
          {
            "name": "jira_search_issues",
            "description": "Searches Jira issues using a JQL query string",
            "inputSchema": {
              "type": "object",
              "properties": {
                "jql":        { "type": "string"  },
                "maxResults": { "type": "integer" }
              },
              "required": ["jql"]
            }
          }
        ]
      }
    }
  }
}
```

---

### Step 3 — Review `McpClientConfig.java` — Creating the SSE Client

Open [McpClientConfig.java](src/main/java/com/workshop/mcp/module02/config/McpClientConfig.java).

```java
@Configuration
public class McpClientConfig {

    @Value("${jira.mcp.server.url}")
    private String jiraMcpServerUrl;

    @Bean(destroyMethod = "close")
    public McpSyncClient jiraMcpClient() {
        var transport = HttpClientSseClientTransport.builder(jiraMcpServerUrl)
                .sseEndpoint("/sse")
                .build();

        var client = McpClient.sync(transport)
                .clientInfo(new McpSchema.Implementation("workshop-jira-client", "1.0.0"))
                .build();

        // MCP handshake: sends initialize request, receives server capabilities
        client.initialize();
        return client;
    }
}
```

**Key concepts:**
- `HttpClientSseClientTransport` uses Java 11+ `HttpClient` internally.
- `client.initialize()` performs the MCP initialization handshake.
- The client is a **singleton** Spring bean — one connection per JVM process.
- For multiple MCP servers, declare multiple `@Bean` methods with `@Qualifier`.

---

### Step 4 — Review `JiraMcpClientService.java` — Tool Discovery and Invocation

Open [JiraMcpClientService.java](src/main/java/com/workshop/mcp/module02/client/JiraMcpClientService.java). This service demonstrates four patterns:

```java
@Service
public class JiraMcpClientService {

    // Pattern 1: Tool Discovery — no hardcoded tool names
    public List<McpSchema.Tool> listAvailableTools() {
        return jiraMcpClient.listTools(null).tools();
    }

    // Pattern 2: Typed Tool Invocation
    public JiraIssueDTO getIssue(String issueKey) {
        var arguments = Map.<String, Object>of("issueKey", issueKey);
        var result = jiraMcpClient.callTool(
                new McpSchema.CallToolRequest("jira_get_issue", arguments));

        if (Boolean.TRUE.equals(result.isError())) {
            throw new JiraMcpException("Failed to fetch issue: " + extractText(result));
        }
        return deserialize(extractText(result), JiraIssueDTO.class);
    }

    // Pattern 3: JQL Search
    public List<JiraIssueDTO> searchCriticalBugs(String projectKey, String fixVersion) {
        String jql = "project=%s AND priority=Critical AND issuetype=Bug AND fixVersion=\"%s\" AND status!=Done"
                .formatted(projectKey, fixVersion);
        var result = jiraMcpClient.callTool(
                new McpSchema.CallToolRequest("jira_search_issues",
                        Map.of("jql", jql, "maxResults", 50)));

        return deserializeList(extractText(result));
    }
}
```

---

### Step 5 — Review `JiraIssueDTO.java` — Response Mapping

Open [JiraIssueDTO.java](src/main/java/com/workshop/mcp/module02/dto/JiraIssueDTO.java).

```java
@JsonIgnoreProperties(ignoreUnknown = true)
public record JiraIssueDTO(
        @JsonProperty("key")       String key,
        @JsonProperty("summary")   String summary,
        @JsonProperty("status")    String status,
        @JsonProperty("priority")  String priority,
        @JsonProperty("issuetype") String issueType,
        @JsonProperty("assignee")  String assignee,
        @JsonProperty("fixVersions") List<String> fixVersions
) {
    public boolean isReleaseBlocker() {
        return "Critical".equalsIgnoreCase(priority)
                && "Bug".equalsIgnoreCase(issueType)
                && !"Done".equalsIgnoreCase(status);
    }
}
```

> **Key concept:** `@JsonIgnoreProperties(ignoreUnknown = true)` is **mandatory** — Jira issues have dozens of optional fields, and MCP servers may return additional metadata. Without this annotation, Jackson throws on unrecognised fields.

---

### Step 6 — Build and Run the Client Demo

```bash
cd module-02-mcp-client
mvn clean package -DskipTests
mvn spring-boot:run
```

**Expected output:**
```
=== Jira MCP Client Demo ===
Available tools: [jira_get_issue, jira_search_issues]

Fetching issue PROJ-101...
Issue: PROJ-101 - NPE in payment processor [CRITICAL BUG, status=Open]

Searching critical bugs for release 2.4...
Found 2 critical open bugs:
  - PROJ-101: NPE in payment processor when card token is null
  - PROJ-108: Deadlock in session management under high concurrency

Release 2.4 is BLOCKED — resolve critical bugs before deploying.

=== Demo Complete ===
```

---

### Step 7 — Inspect Raw SSE Traffic Manually

Open a second terminal and connect to the WireMock SSE endpoint to see what the server sends when a client connects:

```bash
# Open SSE connection (Ctrl+C to close)
curl -N -H 'Accept: text/event-stream' http://localhost:9001/sse
```

**Expected SSE output:**
```
event: endpoint
data: /mcp/message

```

In another terminal, manually send a `tools/list` request:

```bash
curl -s -X POST http://localhost:9001/mcp/message \
  -H 'Content-Type: application/json' \
  -d '{"jsonrpc":"2.0","id":1,"method":"tools/list","params":{}}'
```

> **Key concept:** The SSE endpoint sends two event types:
> - `endpoint` — tells the client where to POST requests
> - `message` — carries JSON-RPC responses back to the client

---

### Step 8 — Test Error Handling — Unknown Issue Key

The WireMock mock returns `isError: true` for unknown issue keys. Verify your client handles this gracefully.

**JSON-RPC exchange:**

Request:
```json
{
  "jsonrpc": "2.0",
  "id": 10,
  "method": "tools/call",
  "params": {
    "name": "jira_get_issue",
    "arguments": { "issueKey": "PROJ-99999" }
  }
}
```

Response from mock:
```json
{
  "jsonrpc": "2.0",
  "id": 10,
  "result": {
    "content": [
      { "type": "text", "text": "Issue PROJ-99999 not found" }
    ],
    "isError": true
  }
}
```

Test it:
```bash
curl -s -X POST http://localhost:9001/mcp/message \
  -H 'Content-Type: application/json' \
  -d '{"jsonrpc":"2.0","id":10,"method":"tools/call","params":{"name":"jira_get_issue","arguments":{"issueKey":"PROJ-99999"}}}'
```

**Expected client behavior:** `JiraMcpClientService.getIssue()` throws `JiraMcpException` with message `"Failed to fetch issue PROJ-99999: Issue PROJ-99999 not found"`.

---

## Troubleshooting

| Problem | Cause | Solution |
|---|---|---|
| Connection refused to `localhost:9001` | WireMock container not running | `docker compose up -d wiremock-jira && docker logs wiremock-jira` |
| `client.initialize()` throws timeout | SSE connection cannot be established | `curl -I http://localhost:9001/sse` — verify `Content-Type: text/event-stream` |
| DTO fields all `null` after deserialization | JSON field names don't match `@JsonProperty` | Log the raw text content: `log.debug("Raw: {}", extractText(result))` |

---

## Extension Challenges

1. Add an **Azure DevOps MCP mock** and register it as a second `McpSyncClient` bean with `@Qualifier`.
2. Implement **retry logic**: if `jira_search_issues` returns `isError: true`, retry up to 3 times with exponential backoff.
3. Add a `/tools` REST endpoint that dynamically proxies tool calls to the Jira MCP server.

---

## Key Takeaways

- MCP Client connects **once via SSE** and reuses the connection for all tool calls — unlike REST which is stateless per-request.
- **Tool discovery** (`tools/list`) at startup enables dynamic behavior — no hardcoded tool names in client code.
- Map JSON-RPC text content to DTOs using Jackson — always use `@JsonIgnoreProperties(ignoreUnknown = true)`.
- `isError: true` in the result means the tool ran but returned a **business error** — handle it in application logic.
- SSE transport is required for microservices; stdio only works for local single-process communication.
