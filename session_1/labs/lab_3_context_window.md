# Lab 3 — Context Window and Memory Limit

**Duration:** 10 minutes  
**Goal:** Observe how what you put in context — and what you leave out — directly changes the quality and accuracy of model responses.

---

## What you will do

- Ask Copilot the same question with no file attached, one file attached, and multiple files attached
- Compare the responses across all three rounds
- Understand what sources Copilot can and cannot see

---

## Background

The **context window** is not memory. The model has no access to files you opened last session, conversations you had yesterday, or code you didn't explicitly attach. It can only use what is inside the current prompt.

When a conversation grows long, earlier content silently falls out. The model does not warn you — it simply starts guessing from its training data, with the same confident tone as when it has the actual information.

---

## Setup

Make sure you have the following file available in this project:

- `session_1/java-demo/src/BudgetCalculator.java`
- `session_1/java-demo/data/sample_budget.csv`

---

## Round 1 — No file attached

1. Open **GitHub Copilot Chat** (`Ctrl+Alt+I`).
2. Start a **new chat** (no prior conversation context).
3. Do **not** attach any file or select any code.
4. Ask exactly this question:

   ```
   What does the BudgetCalculator class do? What methods does it have?
   ```

5. Write down the response. Note specifically:
   - What method names does it list?
   - What behavior does it describe?
   - How confident does the answer sound?

---

## Round 2 — One file attached

1. Start a **new chat**.
2. Click the **paperclip / attach** button and attach `src/BudgetCalculator.java` using `#file`.
3. Ask the **exact same question**:

   ```
   What does the BudgetCalculator class do? What methods does it have?
   ```

4. Write down the response. Compare with Round 1:
   - Are the method names now accurate?
   - Does it reference specific line numbers or logic?

---

## Round 3 — Multiple files attached

1. Start a **new chat**.
2. Attach **all three** files using `#file`:
   - `src/BudgetCalculator.java`
   - `data/sample_budget.csv`
3. Ask the **exact same question**:

   ```
   What does the BudgetCalculator class do? What methods does it have?
   ```

4. Write down the response. Compare with Round 2:
   - Is the answer more accurate, or just longer?
   - Did the extra files help or add noise?

---

## Step 4 — Record and compare

| | Round 1 (no file) | Round 2 (Java file) | Round 3 (multiple files) |
|---|---|---|---|
| Methods listed | | | |
| Accuracy | | | |
| References actual code | | | |
| Response length | | | |

---

## Reference: What Copilot can see

| Source | When included |
|---|---|
| Your chat message | Always |
| Active open file | Partial, when `@workspace` is not used |
| Files attached with `#file` | Explicitly, in full |
| Selected text in editor | When you have a selection active |
| `@workspace` index | Summarized — not full file contents |
| Conversation history | Recent turns only — older turns silently drop out |

---

## Reflection questions

1. Between Round 1 and Round 2, the model gave different answers. What changed?
2. Did adding extra files in Round 3 improve accuracy about the Java class, or did it introduce noise?
3. What does this tell you about how to structure prompts for accuracy?
