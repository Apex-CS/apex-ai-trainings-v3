# Module 02 — Consuming External MCPs: Jira Integration

> **Objective:** Configure a Java MCP Client to connect to an external MCP Server (Jira mock), dynamically discover its tools, invoke them, and map JSON-RPC responses to typed Java DTOs.

- **Duration:** ~60 minutes
- **Difficulty:** Intermediate
- **Practice ID:** M02-P01

---

## Overview

A production Java service needs to query Jira for critical bugs before approving a release. Rather than hardcoding Jira's REST API, you'll connect to a **Jira MCP Server** (implemented as a Spring Boot application) using Spring AI's MCP Client over **SSE transport**. You'll discover tools at runtime, invoke them, and deserialize the JSON responses into typed Java DTOs.

---

## Prerequisites

| Requirement | How to verify |
|---|---|
| Module 01 completed | Understand JSON-RPC basics |
| Docker running | `docker ps` |
| Jira MCP Server started | `docker compose up -d jira-mcp-server` |

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
| Spring AI MCP Server | 1.0.0 — Jira implementation (`spring-ai-mcp-spring-boot-starter`) |
| Jackson Databind | 2.17.x |

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

A Spring Boot MCP Server simulates Jira and speaks the MCP SSE protocol. The server responds to the MCP handshake and Jira-specific tool calls.

```bash
cd /root/projects/apex-ai-trainings-v3/session_4
docker compose up -d jira-mcp-server

# Verify it's up


```

**Expected output:** A JSON health check response with `"status":"UP"` (HTTP 200)

> **Tip:** View the server logs: `docker logs jira-mcp-server -f`

---

### Step 2 — Review Jira MCP Server Implementation — How the Mock Server Works

The `jira-mcp-server` is a Spring Boot application that implements the MCP SSE protocol with Spring AI's MCP Server starter. Examine [jira-mcp-server/src](../jira-mcp-server/src) to see how it handles tool discovery and invocations.

The server responds to `tools/list` with two Jira tools:

- **`jira_get_issue`**: Retrieves a Jira issue by its key (e.g., PROJ-123)  
  - Input: `issueKey` (string, required) — Jira issue key
  - Output: JSON-serialized `JiraIssueDTO` with fields: `key`, `summary`, `status`, `priority`, `issuetype`, `assignee`, `fixVersions`

- **`jira_search_issues`**: Searches Jira issues using a JQL query string  
  - Input: `jql` (string, required), `maxResults` (integer, optional)
  - Output: JSON-serialized list of `JiraIssueDTO`

View the actual tool implementations in [jira-mcp-server/src/main/java](../jira-mcp-server/src/main/java) to see the `@McpFunction` annotations.

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
- `HttpClientSseClientTransport` uses Java 11+ `HttpClient` and establishes a **persistent SSE connection**.
- All JSON-RPC messages (requests and responses) are sent/received as SSE events on this single connection.
- `client.initialize()` performs the MCP initialization handshake (exchanges `initialize` request/response).
- The client is a **singleton** Spring bean — one connection per JVM process.
- For multiple MCP servers, declare multiple `@Bean` methods with `@Qualifier`.
- **Important:** Ensure the server is running and accessible on the configured URL before `initialize()` is called (see Troubleshooting).

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

### Step 7 — Understanding SSE Protocol Communication

The `HttpClientSseClientTransport` establishes a **persistent SSE connection** and communicates bidirectionally:

1. **Establish SSE connection** (GET `/sse`) — returns session ID
2. **Send requests as SSE events** over the persistent connection
3. **Receive responses as SSE events** on the same connection
4. Connection stays open for the lifetime of the client

**Optional: Inspect raw SSE events** (educational only):

```bash
# Terminal 1 — Open SSE connection (keep this running)
curl -N -H 'Accept: text/event-stream' http://localhost:9001/sse
```

**Expected output:**
```
id:1231db5a-729a-4b6f-b6e6-81b1370c381f
event:endpoint
data:/mcp/message?sessionId=1231db5a-729a-4b6f-b6e6-81b1370c381f
```

> **Important:** Do NOT manually POST to the message endpoint — the transport expects bidirectional SSE communication. Manual POSTs will freeze waiting for a response. The transport handles all message routing automatically.

---

### Step 8 — Test Error Handling in the Running Application

The client application (running from Step 6) already tests error scenarios. The `JiraMcpClientService` includes error handling for unknown issue keys:

**Example error scenario:**

When calling `getIssue("PROJ-99999")` on an unknown key, the service:
1. Sends `jira_get_issue` tool call with `issueKey=PROJ-99999`
2. Receives response with `isError: true` and error message
3. Throws `JiraMcpException` with the error details

**To test manually, modify the demo code:**

Edit [JiraDemoRunner.java](src/main/java/com/workshop/mcp/module02/JiraDemoRunner.java) and add:

```java
try {
    var issue = jiraMcpClientService.getIssue("PROJ-99999");
    System.out.println("ERROR: Should have thrown exception");
} catch (JiraMcpException e) {
    System.out.println("✓ Error handling works: " + e.getMessage());
}
```

Then rebuild and run:
```bash
mvn clean package -DskipTests
mvn spring-boot:run
```

**Expected output:**
```
✓ Error handling works: Failed to fetch issue: Issue PROJ-99999 not found
```

---

## Troubleshooting

| Problem | Cause | Solution |
|---|---|---|
| `TimeoutException: Did not observe any item or terminal signal within 20000ms` during app startup | jira-mcp-server not running or unreachable; URL misconfiguration | Verify: `docker compose up -d jira-mcp-server && curl http://localhost:9001/actuator/health` |
| Connection refused to `localhost:9001` | jira-mcp-server container not running | `docker compose up -d jira-mcp-server && docker logs jira-mcp-server` |
| Manual curl POST to `/mcp/message?sessionId=...` freezes | Transport expects persistent SSE connection, not one-off POSTs | Do not manually POST; the transport handles all communication. Test via the running application instead. |
| `client.initialize()` throws timeout | SSE connection cannot be established | `curl -I http://localhost:9001/sse` — verify `Content-Type: text/event-stream` and status 200 |
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
