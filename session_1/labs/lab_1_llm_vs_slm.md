# Lab 1 — LLM vs SLM

**Duration:** 10 minutes  
**Goal:** Run a Small Language Model locally and compare it side-by-side with a cloud LLM.

---

## What you will do

- Install Ollama and pull a local model
- Run a prompt from the terminal
- Connect the local model to VS Code
- Compare the local model with GitHub Copilot

---

## Step 1 — Install Ollama

Ollama lets you run open-source language models locally on your machine.

1. Go to [https://ollama.com/download](https://ollama.com/download) and download the installer for your OS.

   **macOS (via Homebrew):**
   ```bash
   brew install ollama
   ```

   **Windows / Linux:** Download and run the installer from the link above.

2. Verify the installation:
   ```bash
   ollama --version
   ```
   You should see a version number printed. Ollama is now running as a local server.

---

## Step 2 — Pull and run a Small Language Model

1. Pull the Phi-3 Mini model (~2.3 GB download):
   ```bash
   ollama pull phi3:mini
   ```

2. Start an interactive session with the model:
   ```bash
   ollama run phi3:mini
   ```

3. At the prompt, type the following and press Enter:
   ```
   Explain what a token is in the context of language models, in two sentences.
   ```

4. Read the response. Note the quality and speed.

5. Type `/bye` or press `Ctrl+D` to exit the session.

---

## Step 3 — Connect the local model to VS Code

1. Open VS Code.
2. Go to **Extensions** (`Ctrl+Shift+X`) and search for **Continue**.
3. Install the **Continue** extension and open its sidebar.
4. In Continue settings, add a new model:
   - **Provider:** Ollama
   - **Model:** `phi3:mini`
5. Ask the same question from Step 2 inside the Continue chat panel.

You are now running inference entirely on your machine — no cloud, no API key, no cost per query.

---

## Step 4 — Side-by-side comparison

Ask **both** the local model (via Continue) and **GitHub Copilot Chat** the exact same prompt:

```
You are a financial assistant. A user has income of $5000/month, fixed expenses of $2000,
and variable expenses of $1500. Suggest a savings strategy in 3 bullet points.
```

Compare the outputs using the table below:

| Dimension | SLM (Phi-3 Mini, local) | LLM (Copilot / GPT-4) |
|---|---|---|
| Response quality | | |
| Speed | | |
| Cost | | |
| Privacy | | |
| Context window | | |
| Internet required | | |

Fill in your observations as you go.

---

## Reference: Key differences

| Dimension | SLM (Phi-3 Mini, local) | LLM (GPT-4 / Copilot) |
|---|---|---|
| Response quality | Reasonable, may miss nuance | More coherent, better structured |
| Speed | Slower (CPU-bound) | Fast (cloud GPU) |
| Cost | $0 per query | Token-priced |
| Privacy | 100% local | Data sent to vendor |
| Context window | ~4K–8K tokens | 8K–128K+ tokens |
| Internet required | No | Yes |
| Max file/doc size | Small | Much larger |

---

## Reflection questions

1. What differences did you notice in response quality?
2. Were there trade-offs that surprised you?
3. In what kind of project would you choose a local SLM over a cloud LLM?
