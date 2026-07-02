# Lab 5 — RAG vs Fine-Tuning

**Duration:** 7 minutes  
**Goal:** Understand when to use Retrieval-Augmented Generation (RAG) versus fine-tuning, and see how injecting a document changes answer quality and traceability.

---

## What you will do

- Send a prompt that simulates a RAG-style interaction
- Observe how constraining the source affects the model's answer
- Identify when each approach is appropriate

---

## Background

There are two common ways to improve model outputs when the default responses are not good enough:

**RAG (Retrieval-Augmented Generation):** You retrieve a relevant document and inject it into the prompt at query time. The model itself does not change — you just control what it can see before it answers. This keeps answers grounded in your actual content.

**Fine-tuning:** You retrain the model on additional examples to change its behavior, tone, or output format permanently. The model's weights are updated. This is appropriate for stable, repeatable patterns — not for keeping answers current.

---

## When to use each

| Need | Better fit |
|---|---|
| Answer using current internal documents | RAG |
| Use fresh policies, tickets, docs, release notes | RAG |
| Enforce a specific answer format or tone every time | Fine-tuning or system instructions |
| Teach the model a stable, repeatable behavior pattern | Fine-tuning |
| Best enterprise starting point | RAG + instructions + evals |

---

## Exercise — Simulate a RAG interaction

Open **GitHub Copilot Chat** (or Claude/ChatGPT) and send this prompt exactly as written.

Replace `[paste a short internal-safe sample policy]` with the sample policy excerpt below.

```
You are answering using only the pasted policy excerpt below.

1. Answer the user question.
2. Quote the exact section used.
3. If the policy does not answer it, say "Not found in provided context."

Policy excerpt:
--- BEGIN POLICY ---
Section 3.2 — Data Access for Non-Employees
Contractors and third-party vendors are not permitted to access production systems
or production data without explicit written approval from the Data Governance team.
Approval must be renewed every 90 days. Temporary access must be logged in the
access management system within 24 hours of being granted.
--- END POLICY ---

Question:
Can contractors access production data?
```

---

## Step 1 — Send the prompt and record the response

Paste the full prompt above into the chat. Read the response and note:

- Did the model cite the specific section?
- Did it stay within the provided policy, or add information from outside it?
- If you changed the question to something not covered by the policy (e.g., *"Can contractors access staging environments?"*), does it say "Not found in provided context"?

---

## Step 2 — Test the boundary

Send this follow-up in the same chat:

```
Can contractors access the staging environment?
```

Record whether the model:
- Answers from the policy (expected: "Not found in provided context")
- Answers from general knowledge (this would be a RAG failure mode — the model went outside the grounded source)

---

## Step 3 — Record your observations

| | Observation |
|---|---|
| Did the model cite a specific section? | |
| Did it stay within the provided document? | |
| What happened when the answer wasn't in the policy? | |
| Would this response be auditable in a production system? | |

---

## Reflection questions

1. In this exercise, did the model need to be retrained to answer correctly? What made the difference?
2. If your company updates its data access policy every quarter, would RAG or fine-tuning be easier to keep current?
3. What are two risks of letting the model answer from general knowledge instead of a provided document?
