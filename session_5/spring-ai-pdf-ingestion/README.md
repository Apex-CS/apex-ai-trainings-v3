# Spring AI PDF Ingestion Fundamentals

## Overview

This project demonstrates the ingestion side of a RAG architecture.

The application allows you to:

- Upload a PDF
- Extract text
- Generate chunks
- Generate embeddings
- Store vectors in Chroma
- Explore collections stored in Chroma

This project does **not** perform question answering.

Its purpose is to understand how information enters a vector database.

---

# Prerequisites

Install:

- Docker Desktop
- Java 21+
- Maven

Verify:

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

# Clone Repository

```bash
git clone <repository-url>
cd spring-ai-pdf-ingestion
```

---

# Step 1 - Start Chroma and Ollama

Start the required containers:

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

# Step 2 - Install the Embedding Model

This demo uses:

```text
nomic-embed-text
```

to generate embeddings.

Install it:

```bash
docker exec -it ollama ollama pull nomic-embed-text
```

Verify:

```bash
docker exec -it ollama ollama list
```

Expected:

```text
nomic-embed-text:latest
```

---

# Why Is This Model Required?

The application stores vectors in Chroma.

Vectors are generated using:

```text
nomic-embed-text
```

Without this model:

```text
Text
 ↓
Embedding
```

cannot occur.

---

# Step 3 - Start Spring Boot

```bash
mvn clean compile spring-boot:run
```

Expected:

```text
Started PdfIngestionApplication
```

---

# Step 4 - Open Swagger

Open:

```text
http://localhost:7071/swagger-ui/index.html
```

---

# Demo Workflow

## Upload PDF

Endpoint:

```http
POST /api/pdf/upload
```

---

## Extract Text

Endpoint:

```http
GET /api/pdf/text/{fileName}
```

---

## Generate Chunks

Endpoint:

```http
GET /api/pdf/chunks/{fileName}
```

---

## Generate Embeddings

Endpoint:

```http
GET /api/pdf/embeddings/{fileName}
```

---

## Store in Chroma

Endpoint:

```http
POST /api/pdf/store/{fileName}
```

---

## Explore Chroma

List collections:

```http
GET /api/chroma/collections
```

Get collection details:

```http
GET /api/chroma/collections/{collection}
```

Count stored records:

```http
GET /api/chroma/collections/{collection}/count
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

---

# Architecture

```text
PDF
 ↓
Text Extraction
 ↓
Chunking
 ↓
Embedding Generation
 ↓
Chroma
```

This demo intentionally focuses on ingestion and vector storage before introducing retrieval and question-answering.