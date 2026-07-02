# Lab 4 — Hallucination Reduction

**Duration:** 10 minutes  
**Goal:** Trigger a hallucination intentionally, then eliminate it by adding the right file to context — without changing the model or the question.

---

## What you will do

- Ask specific factual questions about `BudgetCalculator.java` with no file in context
- Record the model's answers
- Attach the file and ask the same questions
- Run the actual tests to verify which answers are correct

---

## Background

A hallucination is not a random error. It is a **statistically plausible completion** based on the model's training data. When you ask about a budget calculator without providing the actual code, the model fills in the gaps with what a budget calculator *typically* looks like — not what *your* code actually does.

The fix is not a better model. The fix is putting the right information in context.

---

## Setup

Close all editor tabs and make sure no file is attached or selected before starting Part 1.

Files you will need later:
- `session_1/java-demo/src/BudgetCalculator.java`
- `session_1/java-demo/test/BudgetCalculatorTest.java`

---

## Part 1 — Ask with NO file in context

Open **GitHub Copilot Chat** and start a **new chat** with nothing attached.

Ask each question below and record the model's answer in the table at the end.

**Question A:**
```
In the BudgetCalculator class, what savings rate percentage marks the boundary
between AT_RISK and STABLE? What is the threshold for EXCELLENT?
```

**Question B:**
```
How many monthly periods does calculateAnnualProjection compound over?
```

**Question C:**
```
According to the BudgetCalculator implementation, how many months of expenses
should an emergency fund cover?
```

---

## Part 2 — Attach the file and ask again

1. Start a **new chat**.
2. Attach `src/BudgetCalculator.java` using the `#file` button.
3. Ask the **exact same three questions** and record the answers.

---

## Part 3 — Run the tests to verify

Run the following commands from the `session_1/java-demo/` directory:

```bash
javac -d out src/BudgetCalculator.java test/BudgetCalculatorTest.java
java -cp out BudgetCalculatorTest
```

The tests will confirm the actual values in the code. Use the test output to fill in the "Actual value" column below.

---

## Step 4 — Record and compare

| Question | Answer (no file) | Answer (file attached) | Actual value (from tests) |
|---|---|---|---|
| AT_RISK → STABLE threshold | | | 5% |
| HEALTHY → EXCELLENT threshold | | | 25% |
| Months in `calculateAnnualProjection` | | | 13 (loop runs i <= 12) |
| Emergency fund months | | | 3 |

---

## Reflection questions

1. The model sounded confident in Part 1. How would you have known the answers were wrong without the tests?
2. What changed between Part 1 and Part 2 — the model, the question, or the context?
3. If a developer used the hallucinated value to "fix" a bug in this code, what would happen when the tests run?
4. What habit does this lab suggest you should adopt before asking Copilot factual questions about your own code?
