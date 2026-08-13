# MCP in Java Workshop

> **Model Context Protocol (MCP) Workshop — Java / Spring AI Edition**

## Overview

Five hands-on modules that take you from MCP fundamentals to a production-ready, security-hardened enterprise release agent.

| Module | Title | Duration |
|--------|-------|----------|
| 01 | Foundations — Hello MCP World | ~45 min |
| 02 | Consuming External MCP Servers (Jira) | ~60 min |
| 03 | Building Your Own MCP Server | ~75 min |
| 04 | Security, OAuth2 & Guardrails | ~60 min |
| 05 | End-to-End Lab — Release Integration Agent | ~90 min |

## Tech Stack

- **Java 21** (Virtual Threads / Project Loom)
- **Spring Boot 3.3.x**
- **Spring AI 1.0.0** (`spring-ai-starter-mcp-server`, `spring-ai-starter-mcp-client`)
- **Jackson Databind** for JSON-RPC serialization
- **Spring Security 6** + OAuth2 Resource Server
- **Resilience4j** for rate limiting / circuit breaker
- **WireMock** for mocking Jira / Azure DevOps / legacy REST APIs
- **Testcontainers** for ephemeral Docker services
- **MCP Inspector** (`@modelcontextprotocol/inspector`) for raw traffic inspection

## Prerequisites

- JDK 21+
- Maven 3.9+
- Docker + Docker Compose
- Node.js 18+ (for MCP Inspector)

## Quick Start

```bash
# 1. Clone / open workspace
cd /root/projects/java_workshop

# 2. Start shared infrastructure
docker compose up -d

# 3. Install MCP Inspector globally
npm install -g @modelcontextprotocol/inspector

# 4. Build all modules
mvn clean package -DskipTests

# 5. Each module has its own step-by-step README.md
cat module-01-foundations/README.md
```

## Project Structure

```
java_workshop/
├── pom.xml                          ← Parent POM (dependency management)
├── docker compose.yml               ← WireMock + Keycloak + mock services
├── module-01-foundations/           ← Practice 1: Hello MCP World
├── module-02-mcp-client/            ← Practice 2: Consuming external MCPs
├── module-03-mcp-server/            ← Practice 3: Custom MCP Server
├── module-04-security/              ← Practice 4: OAuth2, Guardrails
└── module-05-e2e-lab/               ← Practice 5: Release Agent
```

## Raw JSON-RPC Protocol (cheat sheet)

```json
// tools/list request
{ "jsonrpc": "2.0", "id": 1, "method": "tools/list", "params": {} }

// tools/call request
{
  "jsonrpc": "2.0", "id": 2,
  "method": "tools/call",
  "params": { "name": "add", "arguments": { "a": 3, "b": 5 } }
}

// tools/call response
{
  "jsonrpc": "2.0", "id": 2,
  "result": { "content": [{ "type": "text", "text": "8.0" }] }
}
```
