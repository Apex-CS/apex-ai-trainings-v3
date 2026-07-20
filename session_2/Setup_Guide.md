# AI Training — Topic 2: Attendee Setup Guide
## Core Java AI Frameworks (Spring AI · LangChain4j · OpenTelemetry · Azure OpenAI)

Welcome! This training is **100% practical**. You can attend without setting anything up — but if you want to **code along and do the hands-on challenge on your own machine**, follow this guide before the session.

⏱ **Total setup time: ~15–20 minutes** (mostly downloads). Do it the day before — not 5 minutes before the session.
💰 **Cost: $0.** Everything runs locally with a free open-source model. No API keys, no cloud account needed.

---

## 1. What you'll need (prerequisites)

| Tool | Minimum version | Check with |
|---|---|---|
| JDK | 21 | `java -version` |
| Maven | 3.9+ | `mvn -version` |
| Docker (with Compose) | any recent | `docker --version` |
| Git | any recent | `git --version` |
| Ollama | latest | `ollama --version` |
| An IDE | IntelliJ IDEA / VS Code / Eclipse | — |

**Hardware:** any laptop with **8 GB RAM** works (16 GB is comfortable). The local AI model needs ~3 GB of disk.

### Installing what's missing

**JDK 21** — use your company's standard, or [SDKMAN](https://sdkman.io) (`sdk install java 21-tem`), or [Adoptium](https://adoptium.net).

**Maven** — `sdk install maven`, or your OS package manager, or [maven.apache.org](https://maven.apache.org/download.cgi).

**Docker** — Docker Desktop (Windows/macOS) or Docker Engine (Linux). If your company uses Rancher Desktop or Podman with compose support, that works too.

**Ollama** (runs AI models locally — this is our free, offline "LLM provider"):
- **macOS:** `brew install ollama` — or download from [ollama.com](https://ollama.com)
- **Windows:** installer from [ollama.com/download](https://ollama.com/download)
- **Linux:** `curl -fsSL https://ollama.com/install.sh | sh`

---

## 2. Setup, step by step

> Run these the day before the training. Steps 3–4 download a few GB — do them on good Wi-Fi.

### Step 1 — Clone the training repository

```bash
git clone <REPO_URL_PROVIDED_BY_TRAINER>
cd ai-training-topic2
```

### Step 2 — Verify the toolchain

```bash
java -version     # must say 21 (or higher)
mvn -version
docker --version
```

If `java -version` shows an older JDK, fix that first — nothing else will work.

### Step 3 — Download the local AI model

```bash
ollama pull llama3.2
```

This downloads ~2 GB once. Then smoke-test it:

```bash
ollama run llama3.2 "Say hello in one sentence"
```

You should get a reply within a few seconds. Type `/bye` to exit.
*(Slow laptop? Use the smaller `ollama pull llama3.2:1b` instead — everything in the training works the same.)*

### Step 4 — Start the observability stack (Grafana + OpenTelemetry)

From the repo root:

```bash
docker compose up -d
```

Wait ~30 seconds, then open **http://localhost:4000** — you should see Grafana (no login needed). Leave it running.

### Step 5 — Build and run the demo app

```bash
cd spring-ai-demo
mvn spring-boot:run
```

First build downloads dependencies (a few minutes). When you see `Started AiTrainingApplication`, test it from a second terminal:

```bash
curl "localhost:8080/ask?q=What+is+JDBC"
```

If you get an answer written by the model: **you're ready.** 🎉

### Step 6 (optional) — The LangChain4j module

```bash
cd ../langchain4j-demo
mvn -q compile exec:java
```

You should see a structured code review printed to the console.

---

**Tips for the session:**
- The **hands-on challenge** (last 10 minutes) is in `CHALLENGES.md`. Pick A (easy), B (medium) or C (spicy). A and B need only what you set up above; C additionally uses a cloud API key the trainer will provide.
- Useful endpoints while experimenting:
  - `GET  localhost:8080/ask?q=...` — free-form chat
  - `POST localhost:8080/review` — body = Java code, returns a typed review (`samples/review.sh` does it for you)
  - `GET  localhost:8080/ops?q=...` — tool-calling demo (try: *"Is payments healthy? Who is on call?"*)
- Watch the app logs: they're set to DEBUG for `org.springframework.ai` so you can see the tool-calling round trip.

---

## 3. Troubleshooting (90% of problems are these)

| Symptom | Fix |
|---|---|
| `Connection refused: localhost:11434` | Ollama isn't running. Start the Ollama app, or run `ollama serve` in a terminal. |
| First `/ask` request takes 30+ s | Normal: the model loads into memory on first use. Warm it up once before the session. |
| App answers are very slow | Use the smaller model: `ollama pull llama3.2:1b`, then change `model: llama3.2` to `llama3.2:1b` in `spring-ai-demo/src/main/resources/application.yml`. |
| `release version 21 not supported` | Maven is using an old JDK. Point `JAVA_HOME` to JDK 21 and re-open the terminal. |
| Port 8080 already in use | Stop the other process, or add `server.port: 8081` to `application.yml` (adjust your curl commands). |
| Port 3000 already in use (Grafana) | Edit `docker-compose.yml`: change `"3000:3000"` to `"3001:3000"`, then use http://localhost:3001. |
| `docker compose` not found | Older Docker: try `docker-compose up -d` (with the hyphen), or update Docker Desktop. |
| Grafana shows no traces | Send a few requests first, then wait ~15 s for traces to appear. |
| Corporate proxy blocks Maven/Ollama downloads | Do steps 3–5 on a network that works (home), or ask the trainer — a pre-warmed setup will be available for pairing. |
| You couldn't set anything up | No problem — come anyway. The challenge can be done in pairs, and the whole session is followable on screen. |

---

## 4. What you should have before the session starts

- [ ] `ollama run llama3.2 "hi"` answers
- [ ] Grafana opens at http://localhost:4000
- [ ] `curl "localhost:8080/ask?q=hello"` returns a model-written answer
- [ ] Repo open in your IDE
- [ ] (Optional) `langchain4j-demo` runs

Questions or stuck? Post in the training channel — don't wait for training day.

See you at the session. Bring an ugly piece of code you'd like reviewed. 🙂
