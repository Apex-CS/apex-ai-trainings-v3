# Module 03 — Building Your Own MCP Server: Customer API Wrapper

> **Objective:** Create a full MCP Server that wraps a legacy Customer REST API, exposing Tools, Resources, and Prompts to LLMs over SSE transport.

- **Duration:** ~75 minutes
- **Difficulty:** Intermediate
- **Practice ID:** M03-P01

---

## Overview

Your company has a Customer Management REST API built in 2015. You need to make it accessible to LLM agents without rewriting it. You'll wrap it in a **Spring AI MCP Server**, exposing CRUD operations as **Tools**, the customer list as a **Resource**, and a support-response template as a **Prompt**. You'll test everything with MCP Inspector.

---

## Prerequisites

| Requirement | How to verify |
|---|---|
| Modules 01 and 02 completed | — |
| Docker running | `docker ps` |
| Customer API mock started | `docker compose up -d wiremock-customer-api` |

---

## Learning Outcomes

- Create a production-ready MCP Server with Spring AI over SSE transport
- Define Tools with complex input/output DTOs and automatic JSON Schema generation
- Expose **Resources** (read-only data) and **Prompts** (templates) from an MCP Server
- Use `RestClient` to call downstream legacy REST APIs from within tool methods
- Choose between stdio and SSE transport based on deployment context
- Test with MCP Inspector connected over HTTP

---

## Tech Stack

| Component | Details |
|---|---|
| Spring Boot | 3.3.5 (`spring-boot-starter-web` for SSE) |
| Spring AI MCP Server | 1.0.0 |
| Spring WebMVC SSE transport | Auto-configured |
| Spring `RestClient` | Spring Boot 3.2+ |
| WireMock | 3.9.1 — mock legacy Customer REST API on port 9002 |

---

## MCP Primitives Covered

| Primitive | Implementation |
|---|---|
| **Tools** | `createCustomer`, `getCustomer`, `listCustomers`, `updateCustomer` |
| **Resources** | `customers://all` — live customer list; `schema://customer` — JSON Schema |
| **Prompts** | `customer_support_response`, `customer_onboarding_email` |

---

## Step-by-Step Instructions

### Step 1 — Start the Legacy Customer REST API Mock

WireMock simulates the legacy Customer REST API that this MCP Server wraps.

```bash
docker compose up -d wiremock-customer-api

# Verify the mock API is up (should return 3 mock customers)
curl -s http://localhost:9002/api/customers | jq '. | length'
```

**Expected output:** `3`

---

### Step 2 — Review `application.yml` — SSE Transport Configuration

Open [application.yml](src/main/resources/application.yml). Unlike Module 01 (stdio), this server uses **SSE transport**. Spring AI auto-configures a WebMVC SSE endpoint.

```yaml
spring:
  ai:
    mcp:
      server:
        name: customer-management-mcp
        version: 1.0.0
        type: SYNC
        sse-message-endpoint: /mcp/message  # POST endpoint for JSON-RPC requests
  threads:
    virtual:
      enabled: true

customer:
  api:
    base-url: http://localhost:9002

server:
  port: 8083
```

**Key concepts:**
- SSE transport requires `spring-boot-starter-web` (WebMVC) — auto-config registers `/sse` and `/mcp/message` endpoints.
- `sse-message-endpoint` defines where clients POST their JSON-RPC requests.
- Virtual Threads allow hundreds of concurrent tool calls without blocking OS threads.

---

### Step 3 — Examine `CustomerDTO.java` — The Domain Object

Open [CustomerDTO.java](src/main/java/com/workshop/mcp/module03/dto/CustomerDTO.java).

```java
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public record CustomerDTO(
        @JsonProperty("id")        String id,
        @JsonProperty("name")      String name,
        @JsonProperty("email")     String email,
        @JsonProperty("phone")     String phone,
        @JsonProperty("tier")      String tier,      // STANDARD, PREMIUM, ENTERPRISE
        @JsonProperty("status")    String status,    // ACTIVE, INACTIVE, SUSPENDED
        @JsonProperty("createdAt") String createdAt,
        @JsonProperty("updatedAt") String updatedAt
) {
    public static CustomerDTO forCreation(String name, String email, String phone, String tier) {
        return new CustomerDTO(null, name, email, phone, tier, "ACTIVE", null, null);
    }
}
```

---

### Step 4 — Examine `CustomerTools.java` — Tools with RestClient

Open [CustomerTools.java](src/main/java/com/workshop/mcp/module03/tools/CustomerTools.java). Each `@Tool` method calls the legacy REST API via Spring `RestClient`.

```java
@Service
public class CustomerTools {

    @Tool(description = """
            Creates a new customer account.
            Required: name (full name), email (unique), phone (E.164 format),
            tier (STANDARD | PREMIUM | ENTERPRISE).
            Returns the created customer with a generated ID.""")
    public String createCustomer(
            @ToolParam(description = "Customer's full name") String name,
            @ToolParam(description = "Email address — must be unique") String email,
            @ToolParam(description = "Phone number in E.164 format, e.g. +12025551234") String phone,
            @ToolParam(description = "Service tier: STANDARD, PREMIUM, or ENTERPRISE") String tier) {
        CustomerDTO created = customerService.create(CustomerDTO.forCreation(name, email, phone, tier));
        return toJson(created);
    }

    @Tool(description = "Retrieves a customer by their unique ID. Returns null if not found.")
    public String getCustomer(
            @ToolParam(description = "Customer UUID, e.g. cust-abc123") String customerId) {
        return customerService.findById(customerId).map(this::toJson).orElse("null");
    }

    @Tool(description = "Lists all customers. Optionally filter by status or tier.")
    public String listCustomers(
            @ToolParam(description = "Filter by status: ACTIVE, INACTIVE, or SUSPENDED. Leave empty for all.") String status,
            @ToolParam(description = "Filter by tier: STANDARD, PREMIUM, or ENTERPRISE. Leave empty for all.") String tier) {
        return toJson(customerService.findAll(status, tier));
    }

    @Tool(description = "Updates an existing customer's details (partial update).")
    public String updateCustomer(
            @ToolParam(description = "Customer UUID to update") String customerId,
            @ToolParam(description = "Updated name, or null to keep current") String name,
            @ToolParam(description = "Updated email, or null to keep current") String email,
            @ToolParam(description = "Updated tier, or null to keep current") String tier) {
        return toJson(customerService.update(customerId, name, email, tier));
    }
}
```

**Discussion points:**
- Tools return `String` (JSON-serialized). This is the MCP-idiomatic approach for complex objects.
- Multi-line tool descriptions help LLMs understand constraints (e.g. email must be unique).
- Partial update pattern: nullable parameters → only non-null fields sent to the REST API.

---

### Step 5 — Examine `CustomerResourceProvider.java` — MCP Resources

Open [CustomerResourceProvider.java](src/main/java/com/workshop/mcp/module03/resources/CustomerResourceProvider.java). Resources expose **read-only context data** to LLMs.

```java
@Configuration
public class CustomerResourceProvider {

    @Bean
    public List<McpServerFeatures.SyncResourceRegistration> customerResources() {
        return List.of(
                // Resource 1: Live customer list
                new McpServerFeatures.SyncResourceRegistration(
                        new McpSchema.Resource(
                                "customers://all",
                                "All Customers",
                                "Complete list of all customers in JSON format",
                                "application/json", null),
                        req -> {
                            String json = serialize(customerService.findAll(null, null));
                            return new McpSchema.ReadResourceResult(List.of(
                                    new McpSchema.TextResourceContents("customers://all", "application/json", json)));
                        }),

                // Resource 2: Customer entity JSON Schema
                new McpServerFeatures.SyncResourceRegistration(
                        new McpSchema.Resource(
                                "schema://customer",
                                "Customer Entity Schema",
                                "JSON Schema definition for the Customer entity",
                                "application/json", null),
                        req -> { /* returns schema string */ })
        );
    }
}
```

> **Resources vs Tools:**
> - **Resources** = READ operations, no side effects. LLMs use them to load context.
> - **Tools** = can modify state. LLMs call them to perform actions.

**`resources/list` JSON-RPC response:**
```json
{
  "jsonrpc": "2.0",
  "id": 5,
  "result": {
    "resources": [
      {
        "uri": "customers://all",
        "name": "All Customers",
        "description": "Complete list of all customers in JSON format",
        "mimeType": "application/json"
      },
      {
        "uri": "schema://customer",
        "name": "Customer Entity Schema",
        "mimeType": "application/json"
      }
    ]
  }
}
```

---

### Step 6 — Examine `CustomerPromptProvider.java` — MCP Prompts

Open [CustomerPromptProvider.java](src/main/java/com/workshop/mcp/module03/prompts/CustomerPromptProvider.java). Prompts are **parameterized message templates**.

```java
@Configuration
public class CustomerPromptProvider {

    @Bean
    public List<McpServerFeatures.SyncPromptRegistration> customerPrompts() {
        return List.of(
                new McpServerFeatures.SyncPromptRegistration(
                        new McpSchema.Prompt(
                                "customer_support_response",
                                "Generates a professional customer support response email",
                                List.of(
                                        new McpSchema.PromptArgument("customer_name", "Customer's full name", true),
                                        new McpSchema.PromptArgument("issue_summary", "Brief description of the issue", true),
                                        new McpSchema.PromptArgument("resolution", "Resolution taken (optional)", false)
                                )),
                        req -> {
                            // Build system + user messages from arguments
                            return new McpSchema.GetPromptResult("...",
                                    List.of(
                                            new McpSchema.PromptMessage(McpSchema.Role.ASSISTANT,
                                                    new McpSchema.TextContent(systemPrompt)),
                                            new McpSchema.PromptMessage(McpSchema.Role.USER,
                                                    new McpSchema.TextContent(userPrompt))));
                        })
        );
    }
}
```

**`prompts/get` request example:**
```json
{
  "jsonrpc": "2.0",
  "id": 7,
  "method": "prompts/get",
  "params": {
    "name": "customer_support_response",
    "arguments": {
      "customer_name": "Alice Johnson",
      "issue_summary": "Unable to login after password reset",
      "resolution": "Password reset link has been re-sent"
    }
  }
}
```

---

### Step 7 — Build and Start the MCP Server

```bash
cd module-03-mcp-server
mvn clean package -DskipTests
mvn spring-boot:run &

# Wait for startup
sleep 5

# Verify the SSE endpoint is available
curl -I http://localhost:8083/sse
```

**Expected:**
```
HTTP/1.1 200
Content-Type: text/event-stream
Transfer-Encoding: chunked
```

---

### Step 8 — Connect MCP Inspector via SSE

Unlike Module 01 (stdio), MCP Inspector connects over HTTP SSE:

```bash
npx @modelcontextprotocol/inspector --web \
  --transport sse \
  --server-url http://localhost:8083/sse
```

**Expected terminal output:**
```
Starting MCP inspector...

MCP Inspector Web is up and running at:
   http://localhost:6274?MCP_INSPECTOR_API_TOKEN=<token>

Opening browser...
```

Open the URL from the terminal in your browser. The Inspector will automatically connect to the server and display the available tools, resources, and prompts in the web UI.

---

### Step 9 — Test All Three MCP Primitives in Inspector

| Primitive | Action | Expected Result |
|---|---|---|
| **Tool** | Call `listCustomers` with no filters | JSON array of 3 customers from WireMock |
| **Tool** | Call `createCustomer`: name=John Doe, email=john@example.com, phone=+1234567890, tier=PREMIUM | JSON with generated `id` and `status=ACTIVE` |
| **Resource** | Read `customers://all` | Full JSON array of customers (read-only, no side effects) |
| **Resource** | Read `schema://customer` | JSON Schema object with `required: [name, email, phone, tier]` |
| **Prompt** | Get `customer_support_response` with `customer_name=Alice`, `issue_summary=Login issue` | Array of two `PromptMessage` objects |

**Sample `tools/call` for createCustomer:**
```json
{
  "jsonrpc": "2.0",
  "id": 3,
  "method": "tools/call",
  "params": {
    "name": "createCustomer",
    "arguments": {
      "name": "John Doe",
      "email": "john@example.com",
      "phone": "+12025551234",
      "tier": "PREMIUM"
    }
  }
}
```

**Response:**
```json
{
  "jsonrpc": "2.0",
  "id": 3,
  "result": {
    "content": [{
      "type": "text",
      "text": "{\"id\":\"cust-xyz\",\"name\":\"John Doe\",\"email\":\"john@example.com\",\"tier\":\"PREMIUM\",\"status\":\"ACTIVE\"}"
    }],
    "isError": false
  }
}
```

---

### Step 10 — stdio vs SSE Transport Decision Guide

| Criteria | `stdio` | `SSE` |
|---|---|---|
| **Best for** | Local CLIs, desktop agents, development | Microservices, Kubernetes, Cloud |
| **Multiple clients** | ❌ Single client only | ✅ Multiple concurrent clients |
| **Network boundary** | ❌ Same machine only | ✅ Works across network |
| **Load balancer** | ❌ Not compatible | ✅ Compatible |
| **Observability** | Log to file only | Standard HTTP logging, tracing |
| **Spring Boot** | No web server needed | Requires `spring-boot-starter-web` |

---

## Troubleshooting

| Problem | Cause | Solution |
|---|---|---|
| Port 8083 already in use | Previous instance running | `lsof -i :8083 \| grep LISTEN \| awk '{print $2}' \| xargs kill -9` |
| Tools return `null` or empty JSON | WireMock customer API not running | `curl http://localhost:9002/api/customers` — if 404, restart: `docker compose up -d wiremock-customer-api` |
| Inspector shows `Resources: 0` or `Prompts: 0` | Bean registration issue | Ensure `@Bean` methods return `List<McpServerFeatures.SyncResourceRegistration>` from a `@Configuration` class |

---

## Extension Challenges

1. Add a `deleteCustomer` tool — include a `⚠️ WARNING: irreversible` in the description (preparation for Module 04).
2. Add a `customers://active` resource that filters only `ACTIVE` customers.
3. Generate the Customer JSON Schema automatically using [victools jsonschema-generator](https://github.com/victools/jsonschema-generator) instead of a hardcoded string.

---

## Key Takeaways

- **SSE transport** enables network-accessible MCP Servers — required for microservices and cloud deployments.
- **Tools** = actions with side effects; **Resources** = read-only context; **Prompts** = reusable LLM templates.
- Tool descriptions should include **constraints and valid enum values** so LLMs know what inputs are acceptable.
- `RestClient` (Spring Boot 3.2+) is the recommended synchronous HTTP client for calling downstream APIs from tool methods.
- Virtual Threads + SSE = high concurrency with minimal boilerplate threading code.
