# Java Demo Project — AI Literacy Training

A minimal Java project used across Labs 2–5 to make AI behavior visible in a real codebase.
No external dependencies. Compile and run with standard `javac` / `java`.

---

## Project structure

```
java-demo/
├── README.md                          ← this file
├── data/
│   └── sample_budget.csv              ← Lab 2 & 3: context window and hallucination
├── src/
│   └── BudgetCalculator.java          ← Lab 3 & 5: edge cases, code review
├── test/
│   └── BudgetCalculatorTest.java      ← Lab 5: agentic workflow, test generation
└── tokenization/
    └── multilingual_snippets.md       ← Lab 1: tokenization comparison
```

---

## How to compile and run (no build tool needed)

```bash
# From the java-demo folder
javac -d out src/BudgetCalculator.java test/BudgetCalculatorTest.java
java -cp out BudgetCalculatorTest
```

Expected output shows PASS / FAIL / WARN lines — intentionally imperfect to drive discussion.

---

## Lab mapping

| Lab | File used | Teaching point |
|-----|-----------|----------------|
| Lab 2 — Context window | `data/sample_budget.csv` + `src/BudgetCalculator.java` | Adding files to context changes model answers |
| Lab 3 — Hallucination | `src/BudgetCalculator.java` | Ask Copilot about the code without/with files selected |
| Lab 4 — RAG vs fine-tuning | `data/sample_budget.csv` | Use CSV as "retrieved doc"; ask policy questions |
| Lab 5 — Agentic workflow | All files | Inspect → plan → modify → test → summarize |

---

## Intentional defects (for Lab 3 and Lab 5)

`BudgetCalculator.java` has six deliberate gaps across six methods. Three are simple edge cases; three are specifically designed to trigger hallucinations when the source file is not in context.

| Method | Defect | Hallucination risk |
|--------|--------|--------------------|
| `calculateMonthlyBudget` | Negative expense values accepted silently | Low — guessable from the name |
| `calculateSavingsRate` | `income = 0` returns `Infinity` | Low — common Java knowledge |
| `applyInflation` | Float drift over many iterations | Low — well-known gotcha |
| `categorizeBudgetHealth` | **Private thresholds are arbitrary company values** | **High** — AI invents plausible-sounding thresholds |
| `calculateAnnualProjection` | **Off-by-one: `i <= 12` runs 13 times** | **High** — AI describes "12 monthly contributions" confidently |
| `parseCSVRow` | No try/catch, no header guard, comma-in-notes fragility | **High** — AI assumes standard defensive parsing |
