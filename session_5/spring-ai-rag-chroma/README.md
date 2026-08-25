# Spring AI RAG Fundamentals Demo with Chroma and Ollama

This project demonstrates how Retrieval-Augmented Generation (RAG) works using:

- Spring Boot
- Spring AI
- Ollama
- ChromaDB

Unlike many RAG examples that hide important implementation details behind framework abstractions, this project intentionally exposes the internal RAG pipeline so students can see exactly how:

- Documents are transformed into embeddings
- Embeddings are stored in a vector database
- Questions are transformed into embeddings
- Similar documents are retrieved
- Prompts are constructed
- LLMs generate answers

---

# Part 1 - Quick Start

## Purpose

This section allows you to run the demo quickly without reading the rest of the document.

Estimated setup time:

```text
5 - 10 minutes
```

---

# Validated Versions

This demo has been validated with:

```text
Java              : 21+
Spring Boot       : 3.5.6
Spring AI         : 1.0.0
ChromaDB          : 1.5.9
Embedding Model   : nomic-embed-text
Chat Model        : llama3.2
```

Using different ChromaDB versions may require API or compatibility changes.

---

# Prerequisites

Install:

- Docker Desktop
- Java 21+
- Maven

Verify your installation:

```bash
docker --version
```

```bash
java -version
```

```bash
mvn -v
```

---

# Clone the Repository

```bash
git clone <repository-url>
cd spring-ai-rag-chroma
```

---

# Docker Configuration

This demo was validated with:

```text
ChromaDB 1.5.9
```

The docker-compose file should contain:

```yaml
version: "3.8"

services:

  chroma:
    image: chromadb/chroma:1.5.9
    container_name: chroma
    ports:
      - "8000:8000"

  ollama:
    image: ollama/ollama
    container_name: ollama
    ports:
      - "11434:11434"
    volumes:
      - ollama:/root/.ollama

volumes:
  ollama:
```

---

# Start Chroma and Ollama

Start the containers:

```bash
docker compose up -d
```

Verify:

```bash
docker ps
```

Expected:

```text
chroma
ollama
```

---

# Important

Starting the containers is not enough.

At this point:

```text
✅ Chroma is running
✅ Ollama is running

❌ Embedding model not installed
❌ Chat model not installed
❌ Collection not created
```

---

# Verify Ollama

```bash
curl http://localhost:11434/api/tags
```

Initially:

```json
{
  "models": []
}
```

This is expected.

---

# Install the Embedding Model

```bash
docker exec -it ollama \
  ollama pull nomic-embed-text
```

Why?

The embedding model converts text into vectors.

Without this model:

```text
Document
   ↓
Embedding
```

cannot happen.

---

# Install the Chat Model

```bash
docker exec -it ollama \
  ollama pull llama3.2
```

Why?

After Chroma retrieves relevant information, the LLM still needs to generate an answer.

Without `llama3.2`, no answer can be generated.

---

# Verify Installed Models

```bash
docker exec -it ollama ollama list
```

Expected:

```text
nomic-embed-text
llama3.2
```

---

# Verify Chroma

```bash
curl http://localhost:8000/api/v2/heartbeat
```

Expected:

```json
{
  "nanosecond heartbeat": ...
}
```

---

# Create the Collection

This project is configured with:

```java
.initializeSchema(false)
```

which means Spring AI expects the collection to already exist.

Create the collection before starting the application:

```bash
curl -X POST \
"http://localhost:8000/api/v2/tenants/default_tenant/databases/default_database/collections" \
-H "Content-Type: application/json" \
-d '{
  "name": "demo-rag"
}'
```

Verify:

```bash
curl http://localhost:8000/api/v2/tenants/default_tenant/databases/default_database/collections
```

Expected:

```json
[
  {
    "name":"demo-rag"
  }
]
```

At this point:

```text
✅ Chroma is running
✅ Ollama is running
✅ nomic-embed-text installed
✅ llama3.2 installed
✅ demo-rag collection exists
```

---

# Start the Application

Launch Spring Boot:

```bash
mvn clean compile spring-boot:run
```

During startup the application automatically:

```text
Employee Handbook
        ↓
Generate Embedding
        ↓
Retrieve Collection UUID
        ↓
Store Document + Embedding in Chroma
        ↓
Application Ready
```

Expected logs:

```text
SOURCE DOCUMENT
```

```text
DOCUMENT EMBEDDING
```

```text
CHROMA COLLECTION
```

```text
DOCUMENT STORED
```

```text
Started AiTrainingApplication
```

Example:

```text
================ CHROMA COLLECTION =================
Collection Name : demo-rag
Collection Id   : xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx
====================================================

================ DOCUMENT STORED ===================
Collection : demo-rag
Collection Id: xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx
Document Id: employee-handbook-1
====================================================
```

---

# Verify the Collection

Open another terminal.

List collections:

```bash
curl http://localhost:8000/api/v2/tenants/default_tenant/databases/default_database/collections
```

Expected:

```json
[
  {
    "name":"demo-rag"
  }
]
```

Inspect collection:

```bash
curl http://localhost:8000/api/v2/tenants/default_tenant/databases/default_database/collections/demo-rag
```

---

# Example Queries

Use these examples to validate retrieval and generation.

## PTO Policy

```bash
curl "http://localhost:8080/api/rag?question=How%20many%20PTO%20days%20do%20employees%20receive?"
```

Expected answer:

```text
Employees receive 20 PTO days annually.
```

---

## PTO Carry-Over Policy

```bash
curl "http://localhost:8080/api/rag?question=Can%20PTO%20days%20be%20carried%20over?"
```

Expected answer:

```text
Employees may carry over up to 5 PTO days into the following year.
```

---

## Remote Work Policy

```bash
curl "http://localhost:8080/api/rag?question=How%20many%20remote%20work%20days%20are%20allowed?"
```

Expected answer:

```text
Employees may work remotely up to three days per week.
```

---

## Travel Policy

```bash
curl "http://localhost:8080/api/rag?question=Is%20manager%20approval%20required%20for%20business%20travel?"
```

Expected answer:

```text
Manager approval is required before any business travel is booked.
```

---

## Semantic Search Example

```bash
curl "http://localhost:8080/api/rag?question=Do%20I%20need%20approval%20before%20booking%20a%20trip?"
```

Expected behavior:

```text
The Travel Policy should be retrieved even though
the wording differs from the original document.
```

---

## Another Semantic Search Example

```bash
curl "http://localhost:8080/api/rag?question=How%20often%20can%20employees%20work%20from%20home?"
```

Expected behavior:

```text
The Remote Work Policy should be retrieved even though
the handbook uses the term "remote work".
```

---

# Stop the Environment

Stop Spring Boot:

```text
CTRL + C
```

Stop containers:

```bash
docker compose down
```

To completely reset the environment:

```bash
docker compose down -v
```

---

# Part 2 - Deep Dive