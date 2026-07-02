# Lab 2 — Tokenization Demo

**Duration:** 10 minutes  
**Goal:** See how text is broken into tokens, and understand why language and code structure affect cost and context limits.

---

## What you will do

- Use an online tokenizer to count tokens for different inputs
- Compare token counts across English, Spanish, and code
- Draw conclusions about cost and context efficiency

---

## Background

Models never process your raw text. Before any inference happens, your input is split into **tokens** — small units that may be full words, word fragments, punctuation marks, or whitespace characters.

This matters because:
- Context windows are measured in **tokens**, not words or characters
- API usage is billed **per token** (input + output)
- Different languages and writing styles produce very different token counts for the same meaning

---

## Step 1 — Open a tokenizer tool

Open one of these tokenizer tools in your browser:

- Claude tokenizer: [https://www.claudetokenizer.com](https://www.claudetokenizer.com)
- GPT tokenizer: [https://gpt-tokenizer.dev/](https://gpt-tokenizer.dev/)

---

## Step 2 — Tokenize the sample inputs

Paste each of the three inputs below **one at a time** into the tokenizer and record the token count.

**Input A — English:**
```
Hello, I need help creating a monthly budget.
```

**Input B — Spanish (same meaning):**
```
Hola, necesito ayuda para crear un presupuesto mensual.
```

**Input C — Code:**
```
function calculateMonthlyBudget(income, fixedExpenses, variableExpenses) { return income - fixedExpenses - variableExpenses; }
```

---

## Step 3 — Record your results

| Input | Language / Type | Token count |
|---|---|---|
| Input A | English prose | |
| Input B | Spanish prose | |
| Input C | JavaScript code | |

---

## Step 4 — Observe the tokenization

Most tokenizers highlight each token with a different color. Look at how each input is split:

- Does the Spanish input split more words into fragments compared to English?
- How is the identifier `calculateMonthlyBudget` split?
- Are punctuation marks and spaces their own tokens?

Write down at least **two observations** about how the tokenization differs across the three inputs.

---

## Reflection questions

1. Inputs A and B express the same idea. If Input B has more tokens, what does that mean for the cost of a Spanish-language application vs. an English one?
2. The code identifier `calculateMonthlyBudget` may split into 4 or more tokens. How would that affect a prompt that includes an entire codebase?
3. If your context window is 8,000 tokens and your prompt takes 6,000, how much room is left for the model's answer?
