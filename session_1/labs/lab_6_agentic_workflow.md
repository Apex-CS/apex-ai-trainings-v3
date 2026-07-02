# Lab 6 — Agentic Copilot Workflow

**Duration:** 8 minutes  
**Goal:** Create reusable, structured prompt files and workspace instructions that turn Copilot into a constrained, repeatable AI collaborator.

---

## What you will do

- Create a `copilot-instructions.md` file that sets baseline behavior for Copilot across your workspace
- Create two reusable prompt files: one for code review, one for test generation
- Use both prompt files on `BudgetCalculator.java` and compare results

---

## Background

An agent is a model that takes multiple steps, uses tools, and acts on your behalf. The risk is not its power — it's that it can take actions confidently when it's wrong. Structured instructions and constrained scope are how you manage that risk.

**What you are building:**
- **Custom instructions** — rules of engagement that apply every time Copilot acts in this workspace
- **Prompt files** — reusable, versioned job descriptions with explicit output requirements

---

## Step 1 — Create the workspace instructions file

1. In the project root, create the folder `.github/` if it does not already exist.
2. Inside `.github/`, create a file called `copilot-instructions.md`.
3. Paste the following content:

   ```markdown
   # Project AI instructions

   Always:

   - Explain assumptions before changing code.
   - Prefer small, reviewable changes.
   - Do not invent APIs or dependencies.
   - Run or suggest tests after changes.
   - If information is missing, ask for the specific file or command needed.
   ```

4. Save the file. Copilot will now apply these rules to every interaction in this workspace.

---

## Step 2 — Create the code review prompt file

1. Create the folder `.github/prompts/` if it does not already exist.
2. Inside `.github/prompts/`, create a file called `review-code.prompt.md`.
3. Paste the following content:

   ```markdown
   # Code review prompt

   Review the selected code for:
   - Correctness
   - Security
   - Error handling
   - Maintainability
   - Missing tests

   Return:
   1. Critical issues
   2. Suggested improvements
   3. Test cases to add
   4. Questions or assumptions
   ```

4. Save the file.

---

## Step 3 — Create the test generation prompt file

1. Inside `.github/prompts/`, create a file called `create-tests.prompt.md`.
2. Paste the following content:

   ```markdown
   # Test generation prompt

   Given the selected code:
   1. Identify all boundary conditions and edge cases.
   2. Generate test cases for each.
   3. Use the existing test style and structure in this project.
   4. Flag any behavior that appears to be a defect rather than a design decision.
   ```

3. Save the file.

---

## Step 4 — Run a code review using the prompt file

1. Open `session_1/java-demo/src/BudgetCalculator.java` in the editor.
2. Select all the code (`Ctrl+A`).
3. Open **Copilot Chat**.
4. Reference the prompt file by typing:
   ```
   #file:.github/prompts/review-code.prompt.md
   ```
   Then send it.
5. Read the response. Note:
   - Does Copilot follow the structured output format (Critical issues → Improvements → Tests → Assumptions)?
   - Does it flag the off-by-one in `calculateAnnualProjection`?
   - Does it flag the threshold constants?

---

## Step 5 — Generate tests using the prompt file

1. With `BudgetCalculator.java` still open and selected.
2. Open a new Copilot Chat message.
3. Reference the test prompt:
   ```
   #file:.github/prompts/create-tests.prompt.md
   ```
   Then send it.
4. Check if the generated tests:
   - Cover boundary values for the savings rate thresholds
   - Include an edge case for the loop count in `calculateAnnualProjection`
   - Match the style of the existing `BudgetCalculatorTest.java`

---

## Step 6 — Record your results

| | Observation |
|---|---|
| Did the review use the structured output format? | |
| Did it flag the off-by-one? | |
| Did the generated tests cover boundaries? | |
| Did the instructions file change Copilot's default behavior? | |

---

## Reflection questions

1. Without the prompt file, would Copilot have returned output in the same structured format? What does that tell you about repeatability?
2. What is the difference between asking Copilot to "review my code" versus using a structured prompt file?
3. Why is it important that Copilot states its assumptions before making changes, as required by the instructions file?
4. In a team setting, what is the benefit of keeping these prompt files in version control alongside the code?
