# Module 01 — Foundations: Hello MCP World

> **Objective:** Establish MCP theory and observe raw JSON-RPC traffic with a live Java server and MCP Inspector.

- **Duration:** ~45 minutes
- **Difficulty:** Beginner
- **Practice ID:** M01-P01

---

## Overview

Build a minimal MCP Server in Java using Spring AI, expose Calculator and System-Info tools over **stdio transport**, then use **MCP Inspector** to observe every JSON-RPC message in real time. This practice exposes the "under the hood" mechanics before any abstraction hides them.

---

## Prerequisites

| Requirement | Verify with |
|---|---|
| JDK 21+ | `java -version` → `openjdk 21` |
| Maven 3.9+ | `mvn -version` |
| Node.js 18+ | `node --version` |
| Git | `git --version` |

---

## Learning Outcomes

- Understand the three MCP primitives: **Tools**, **Resources**, and **Prompts**
- Read and interpret raw JSON-RPC 2.0 messages
- Annotate Java methods with `@Tool` and `@ToolParam`
- Configure a stdio MCP Server with Spring AI
- Use MCP Inspector to browse and invoke tools interactively

---

## Tech Stack

| Component | Version |
|---|---|
| Spring Boot | 3.3.5 |
| Spring AI (MCP Server) | 1.0.0 |
| Java Virtual Threads | JDK 21 |
| MCP Inspector | `@modelcontextprotocol/inspector` **2.0.0** |

---

## Step-by-Step Instructions

### Step 1 — Install MCP Inspector

MCP Inspector is a Node.js CLI tool that acts as an MCP client. It connects to your server, lists tools, and lets you call them interactively while showing raw JSON-RPC messages in its browser UI.

```bash
npm install -g @modelcontextprotocol/inspector
npm list -g @modelcontextprotocol/inspector
```

**Expected output:**
```
/root/.nvm/versions/node/.../lib
└── @modelcontextprotocol/inspector@2.0.0
```

> **Note:** In v2.0.0 the `--version` flag does not print a version — it starts the web server instead. Use `npm list -g` to check the installed version.

> **Tip:** If npm global installs fail due to permissions:
> ```bash
> npm install -g @modelcontextprotocol/inspector --prefix ~/.npm-global
> ```

---

### Step 2 — Review `pom.xml` — MCP Server Dependency

Open [module-01-foundations/pom.xml](pom.xml). The single MCP dependency that does all the heavy lifting is `spring-ai-starter-mcp-server`. It pulls in the JSON-RPC engine, transport layer, and Spring Boot auto-configuration.

```xml
<dependency>
    <groupId>org.springframework.ai</groupId>
    <artifactId>spring-ai-starter-mcp-server</artifactId>
</dependency>
```

> **Key concept:** This single starter replaces hundreds of lines of manual JSON-RPC parsing code. Under the hood it registers `McpServer` beans and wires the transport layer automatically.

---

### Step 3 — Examine `CalculatorTools.java` — Your First `@Tool`

Open [CalculatorTools.java](src/main/java/com/workshop/mcp/module01/tools/CalculatorTools.java). Notice how a plain Java method becomes an MCP tool via `@Tool` and `@ToolParam` annotations. Spring AI uses reflection + Jackson to automatically generate the JSON Schema that MCP requires for the tool's input parameters.

```java
@Service
public class CalculatorTools {

    @Tool(description = "Adds two numbers and returns the sum")
    public double add(
            @ToolParam(description = "First operand") double a,
            @ToolParam(description = "Second operand") double b) {
        return a + b;
    }

    @Tool(description = "Divides dividend by divisor. Returns error if divisor is zero.")
    public String divide(
            @ToolParam(description = "Dividend") double dividend,
            @ToolParam(description = "Divisor — must not be zero") double divisor) {
        if (divisor == 0) {
            return "Error: division by zero is not allowed";
        }
        return String.valueOf(dividend / divisor);
    }
}
```

**Discussion points:**
- Why does `divide()` return `String` instead of `double`? Because tools should return human-readable content for LLMs.
- The `@ToolParam` description is what the LLM reads to decide which argument maps to which parameter.
- Auto-generated JSON Schema for `add()`: `{ type: 'object', properties: { a: { type: 'number' }, b: { type: 'number' } } }`

---

### Step 4 — Examine `EchoTools.java`

Open [EchoTools.java](src/main/java/com/workshop/mcp/module01/tools/EchoTools.java). Demonstrates tools that return structured JSON strings (which LLMs can parse) and zero-parameter tools.

```java
@Service
public class EchoTools {

    @Tool(description = "Echoes the input message back to the caller unchanged")
    public String echo(@ToolParam(description = "The message to echo back") String message) {
        return "Echo: " + message;
    }

    @Tool(description = "Returns the current server timestamp in ISO-8601 UTC format")
    public String currentTimestamp() {
        return Instant.now().toString();
    }

    @Tool(description = "Returns JVM version, OS, CPU count, and heap memory as a JSON object")
    public String systemInfo() {
        return """
                {
                  "javaVersion": "%s",
                  "osName": "%s",
                  "availableProcessors": %d,
                  "maxHeapMemoryMb": %d,
                  "threadModel": "%s"
                }""".formatted(
                System.getProperty("java.version"),
                System.getProperty("os.name"),
                Runtime.getRuntime().availableProcessors(),
                Runtime.getRuntime().maxMemory() / (1024 * 1024),
                Thread.currentThread().isVirtual() ? "Virtual Thread" : "Platform Thread");
    }
}
```

---

### Step 5 — Review `McpServerConfig.java` — Registering Tools

Open [McpServerConfig.java](src/main/java/com/workshop/mcp/module01/config/McpServerConfig.java). Spring AI discovers tools via `ToolCallbackProvider` beans.

```java
@Configuration
public class McpServerConfig {

    @Bean
    public ToolCallbackProvider calculatorToolCallbacks(CalculatorTools calculatorTools) {
        return MethodToolCallbackProvider.builder()
                .toolObjects(calculatorTools)
                .build();
    }

    @Bean
    public ToolCallbackProvider echoToolCallbacks(EchoTools echoTools) {
        return MethodToolCallbackProvider.builder()
                .toolObjects(echoTools)
                .build();
    }
}
```

> **Key concept:** `MethodToolCallbackProvider` uses reflection to discover `@Tool`-annotated methods, generates their JSON Schema from `@ToolParam` annotations, and creates `ToolCallback` objects the MCP engine dispatches at `tools/call` time.

---

### Step 6 — Review `application.yml` — stdio Transport Configuration

Open [application.yml](src/main/resources/application.yml). The server reads JSON-RPC requests from stdin and writes responses to stdout.

```yaml
spring:
  ai:
    mcp:
      server:
        name: workshop-hello-mcp
        version: 1.0.0
        type: SYNC
        stdio: true       # Enable stdio transport

logging:
  level:
    root: OFF             # CRITICAL — suppress ALL stdout output
  file:
    name: /tmp/module01-mcp-server.log
```

> ⚠️ **CRITICAL:** When using stdio transport, **ALL application logs MUST be redirected to a file**. Any text written to stdout breaks the JSON-RPC framing and corrupts communication with the client.

**Common mistakes:**
| Mistake | Consequence |
|---|---|
| `logging.level.root=INFO` with stdio | Client receives corrupted JSON |
| `System.out.println()` inside a tool | Corrupts the JSON-RPC stream |
| Missing `stdio: true` | Server won't read from stdin |

---

### Step 7 — Build the Module

```bash
cd module-01-foundations
mvn clean package -DskipTests
ls -lh target/module-01-foundations-*.jar
```

**Expected output:**
```
-rw-r--r-- 1 user user 28M ... target/module-01-foundations-1.0.0-SNAPSHOT.jar
BUILD SUCCESS
```

> Verify the JAR is > 20 MB — that confirms it's a fat JAR with all dependencies bundled.

---

### Step 8 — Connect MCP Inspector to the Server

Open a **new terminal window** and run:

```bash
npx @modelcontextprotocol/inspector --web \
  --transport stdio \
  -- java -jar target/module-01-foundations-1.0.0-SNAPSHOT.jar
```

**Expected terminal output:**
```
Starting MCP inspector...

MCP Inspector Web is up and running at:
   http://localhost:6274?MCP_INSPECTOR_API_TOKEN=<token>

   Sandbox (MCP Apps): http://localhost:33175/sandbox

   Auth token: <token>

Opening browser...
```

MCP Inspector opens a web UI at **http://localhost:6274** (copy the full URL with the token from the terminal).

> **What changed in v2.0.0:**
> - Port is now `6274` (was `5173`)
> - Modes: `--web` (browser UI), `--cli` (terminal), `--tui` (interactive terminal)
> - Stdio command is passed after `--` separator
> - A one-time auth token is required in the URL

> **Tips:**
> - The server process is spawned as a child — killing the Inspector also kills the server
> - Use `Ctrl+C` to exit cleanly

---

### Step 9 — Observe the `tools/list` JSON-RPC Exchange

In MCP Inspector, navigate to the **Tools** tab, then switch to the **Messages / Protocol** tab to see the raw JSON-RPC exchange that happened automatically on connection.

**Request sent by Inspector:**
```json
{
  "jsonrpc": "2.0",
  "id": 1,
  "method": "tools/list",
  "params": {}
}
```

**Response from server:**
```json
{
  "jsonrpc": "2.0",
  "id": 1,
  "result": {
    "tools": [
      {
        "name": "add",
        "description": "Adds two numbers and returns the sum",
        "inputSchema": {
          "type": "object",
          "properties": {
            "a": { "type": "number", "description": "First operand" },
            "b": { "type": "number", "description": "Second operand" }
          },
          "required": ["a", "b"]
        }
      },
      {
        "name": "divide",
        "description": "Divides dividend by divisor. Returns error if divisor is zero.",
        "inputSchema": {
          "type": "object",
          "properties": {
            "dividend": { "type": "number" },
            "divisor":  { "type": "number" }
          },
          "required": ["dividend", "divisor"]
        }
      },
      {
        "name": "echo",
        "description": "Echoes the input message back to the caller unchanged",
        "inputSchema": {
          "type": "object",
          "properties": {
            "message": { "type": "string" }
          },
          "required": ["message"]
        }
      }
    ]
  }
}
```

**Discussion questions:**
1. How does an LLM know which tool to use? → It reads the `"description"` field.
2. How does an LLM know what arguments to pass? → It reads the `"inputSchema"` JSON Schema.
3. What happens if `"required"` is omitted? → The LLM may omit mandatory arguments.

---

### Step 10 — Invoke the `add` Tool and Read the JSON-RPC Call

In MCP Inspector, select the **`add`** tool, enter `a = 10`, `b = 32`, and click **Execute**. Watch the Protocol tab.

**Request:**
```json
{
  "jsonrpc": "2.0",
  "id": 3,
  "method": "tools/call",
  "params": {
    "name": "add",
    "arguments": { "a": 10, "b": 32 }
  }
}
```

**Response:**
```json
{
  "jsonrpc": "2.0",
  "id": 3,
  "result": {
    "content": [
      { "type": "text", "text": "42.0" }
    ],
    "isError": false
  }
}
```

✅ **Verify:** result is `42.0`

---

### Step 11 — Trigger a Tool Error and Observe `isError: true`

Call **`divide`** with `dividend = 10`, `divisor = 0`.

**Request:**
```json
{
  "jsonrpc": "2.0",
  "id": 4,
  "method": "tools/call",
  "params": {
    "name": "divide",
    "arguments": { "dividend": 10, "divisor": 0 }
  }
}
```

**Response:**
```json
{
  "jsonrpc": "2.0",
  "id": 4,
  "result": {
    "content": [
      { "type": "text", "text": "Error: division by zero is not allowed" }
    ],
    "isError": true
  }
}
```

> **Key concept:** `isError: true` signals to the LLM that the tool ran but returned a logical error. The LLM can then retry with different arguments or explain the error to the user. Reserve JSON-RPC `error` objects for *protocol-level* failures only.

---

### Step 12 — Verify Virtual Threads via `systemInfo`

Call the **`systemInfo`** tool and inspect the response. Then verify the server is using Virtual Threads:

```bash
tail -20 /tmp/module01-mcp-server.log | grep -i thread
```

**Expected:**
```
Thread[#42,virtual-thread-1,5,main]
```

> **Key concept:** Virtual Threads (JDK 21 / Project Loom) allow the MCP Server to handle hundreds of concurrent tool calls with minimal memory overhead. Each tool call is dispatched on a new virtual thread — ideal for I/O-bound calls to downstream APIs.

---

## Troubleshooting

| Problem | Likely Cause | Solution |
|---|---|---|
| Inspector shows "Connection refused" or hangs | JAR path wrong or Java not in PATH | Verify: `java -jar target/module-01-foundations-1.0.0-SNAPSHOT.jar` (exits immediately in stdio mode) |
| Inspector connects but shows 0 tools | `ToolCallbackProvider` beans not registered | Check `McpServerConfig.java` — ensure `@Bean` methods return `MethodToolCallbackProvider` with the correct tool objects |
| JSON parse error in Inspector | Log output leaking to stdout | Verify `application.yml` has `logging.level.root: OFF` and `logging.file.name` set |

---

## Extension Challenges

1. Add a `fibonacci` tool that uses Java 21 records for the response object.
2. Add a **Resource** (`resources/list`) that exposes a static `calculator-help` text document.
3. Add a **Prompt** template for `explain-calculation` that guides the LLM on how to use the calculator tools.

---

## Key Takeaways

- MCP is **JSON-RPC 2.0 over a transport** — nothing more, nothing less.
- `@Tool` + `@ToolParam` auto-generate the JSON Schema that LLMs use to understand tool parameters.
- **stdio transport**: stdin → JSON-RPC request, stdout → JSON-RPC response. All logs **MUST** go to a file.
- Tool errors use `isError: true` inside `result.content` — protocol errors use JSON-RPC `error` objects.
- Virtual Threads make MCP Servers naturally concurrent with zero explicit threading code.
