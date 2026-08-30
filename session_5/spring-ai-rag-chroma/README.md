# Create the Collections

This project is configured with:

```java
.initializeSchema(false)
```

which means Spring AI expects the collections to already exist.

Both collections are required before starting the application.

Create `demo-rag`:

```bash
curl -X POST \
"http://localhost:8000/api/v2/tenants/default_tenant/databases/default_database/collections" \
-H "Content-Type: application/json" \
-d '{
  "name": "demo-rag"
}'
```

Create `FastShow-collection`:

```bash
curl -X POST \
"http://localhost:8000/api/v2/tenants/default_tenant/databases/default_database/collections" \
-H "Content-Type: application/json" \
-d '{
  "name": "FastShow-collection"
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
  },
  {
    "name":"FastShow-collection"
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
✅ FastShow-collection exists
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