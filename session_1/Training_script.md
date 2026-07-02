# Practical AI Literacy training script

## Introduction

- **Time**: 0–5 min
- **Section**: Setup and framing
- **Practical Outcome**: Participants understand the lab goal: “AI is probabilistic, context-bound, token-priced, and needs validation.”

### Opening

*Good morning/afternoon everyone,*
*Welcome to this AI training series.*
*Before we begin, I’d like to quickly frame what this initiative is about.*
*We are a group within the Java Practice, and this series is something we’ve put together to share practical knowledge about AI topics, grounded in real development scenarios.*
*The goal is not to turn anyone into machine learning experts—but to help us, as Java developers, understand how to use AI effectively in our day-to-day work.*
*Across these 7 sessions, we’ll focus on hands-on examples, showing how AI behaves in real situations—especially the kinds of problems we encounter in projects:*

- Unexpected outputs
- Misleading answers
- Cost and performance trade-offs
- And integration patterns in development workflows

*Today’s session is about building intuition.*
*Most of you have already been exposed to the theory—models, transformers, architectures.*
*But in practice, most issues we see are not theoretical—they’re practical:*

- Prompts that are unclear or too long
- Context that gets ignored
- Outputs that look right but are actually wrong
- Costs that grow unexpectedly

*So today is not about how AI works internally.*
*It’s about how it behaves when you use it..*

*Throughout the session, I want you to keep four mental models in mind. Everything we’ll see today maps to these:*

1.- AI is probabilistic
It does not ‘know’ things—it predicts likely tokens.

2.- AI is context-bound
It only uses what fits in its context window. Anything outside is invisible.

3.- AI is token-priced
Every interaction has a cost—input tokens, output tokens, and sometimes hidden overhead.

4.- AI requires validation
Even when it sounds correct, it can be wrong. Especially when it sounds very confident.

*If you remember nothing else from today, remember these four.*

*We’ll primarily use VS Code with GitHub Copilot.*
*But conceptually, everything you’ll see applies equally to:*

- ChatGPT
- Claude
- Gemini

*The interface may change, but:*

- Tokens behave the same way
- Context limitations exist everywhere
- Hallucinations happen across all models

*So think of Copilot as the execution environment, not the concept itself.*

*This session is structured as six short labs.*
*Each lab focuses on one concept and shows it live, so you can see cause and effect immediately.*
*The goal is not to memorize commands—it’s to develop intuition:*

- If I do this → the model behaves like that
- If I change this → the output changes in this way

That intuition is what allows you to use AI effectively in real projects.

Here's how the session will flow:

- First, we'll compare a local Small Language Model to a cloud LLM — to understand what "size" actually means in practice.
- Then we'll look at tokens and tokenization, and why English, Spanish, and code behave differently.
- Then we'll explore context limits and cost, and why long prompts don't always help.
- After that, we'll experiment with hallucinations — same prompt, different outputs depending on what's in context.
- Then we'll compare RAG vs fine-tuning, and when each approach makes sense.
- Finally, we'll build a simple agentic workflow, where Copilot doesn't just answer — it plans, modifies, and verifies.

Each step builds on the previous one. A quick expectation-setting before we start:

- You will see outputs that are wrong
- You will see behavior that feels inconsistent
- You will see cases where adding more information makes things worse

This is not the tool failing — this is the tool behaving as designed. The goal is to learn how to recognize and control that behavior, not eliminate it.

***

**Transition to Lab 1:**

*Let's start by grounding ourselves in something concrete: what exactly is the difference between a "small" and a "large" language model? You've probably heard the term LLM — Large Language Model. But lately you also hear SLM — Small Language Model. Are they the same thing? Can you run one locally? What do you actually give up? Let's find out.*

***

## Lab 1 - LLM vs SML

- **Time**: 5–15 min
- **Section**: SLM vs LLM comparison
- **Practical Outcome**: Participants understand what model size means in practice, experience a local SLM firsthand, and understand the tradeoffs vs cloud LLMs.

### Spoken intro

The models you use in Copilot or ChatGPT run on massive infrastructure — hundreds of billions of parameters, GPU clusters, low-latency APIs. They're powerful, but they're also remote, token-priced, and dependent on network access and vendor availability.

Small Language Models — SLMs — are models compact enough to run locally on a laptop. We're talking 1 to 7 billion parameters instead of hundreds of billions. They're not as capable, but they're fast, private, free to run, and don't require internet access.

Today we'll install one in about two minutes using a tool called Ollama. The goal isn't to replace your cloud LLM — it's to understand what you're trading off when you choose one over the other.

### Step 1 — Install Ollama

Ollama is a tool that lets you run open-source language models locally. Think of it as Docker, but for AI models. One command to install, one command to pull a model, and you're running inference on your machine.
Go to https://ollama.com and download the installer for your OS, or run:

```bash
# macOS (via Homebrew)
brew install ollama

# Windows / Linux: download from https://ollama.com/download
```

Verify it's running:

```bash
ollama --version
```

You should see a version number. That's it — the server is running locally.

### Step 2 — Pull and run a Small Language Model

Now let's pull a small model. We'll use Phi-3 Mini from Microsoft — it's 3.8 billion parameters, about 2.3 GB on disk, and it runs on CPU if you don't have a GPU. That's the entire model, weights included, on your laptop.

```bash
ollama pull phi3:mini
ollama run phi3:mini
```

You'll get an interactive prompt. Ask it something simple:

```bash
>>> Explain what a token is in the context of language models, in two sentences.
```

### Step 3 — Use the SLM from VS Code

Now let's connect this local model to VS Code so you can use it the same way you use Copilot.
Install the Continue extension in VS Code (or use Ollama's native OpenAI-compatible endpoint):

1. Open VS Code → Extensions → search Continue
2. Install and open the Continue sidebar
3. In Continue settings, add a new model:
    - Provider: Ollama
    - Model: phi3:mini
4. Start chatting directly inside VS Code

Ask the same question you asked before. You'll get a response — from your own machine, with no cloud dependency.

### Step 4 — Side-by-side comparison

Now ask both the local SLM and GitHub Copilot (or ChatGPT if available) the same prompt:

```bash
You are a financial assistant. A user has income of $5000/month, fixed expenses of $2000, 
and variable expenses of $1500. Suggest a savings strategy in 3 bullet points.
```

|Dimension|SLM (Phi-3 Mini, local)|LLM (GPT-4 / Copilot)|
|--|--|--|
|Response quality|Reasonable, may miss nuance|More coherent, better structured|
|Speed|Slower (CPU-bound)|Fast (cloud GPU)|
|Cost|$0 per query|Token-priced|
|Privacy|100% local|Data sent to vendor|
|Context window|~4K–8K tokens|8K–128K+ tokens|
|Internet required|No|Yes|
|Max file/doc size it can handle|Small|Much larger|

### Teaching point

The size of a model is not just a marketing number — it has real implications for what you can run where, what it costs, and what guarantees you have about data leaving your environment.

SLMs are a valid choice for: offline scenarios, sensitive data, CI/CD pipelines, edge devices, or cost-constrained applications. LLMs are better when you need higher reasoning quality, larger context, or multimodal capabilities.

The key insight is this: "intelligence" in these systems is largely a function of the number of parameters — the learned weights — and the quality of training data. A smaller model has fewer patterns to draw from. That's not a flaw, it's a tradeoff.

Now, both SLMs and LLMs share something fundamental: they operate on tokens. And tokens are the single most important thing to understand if you want to use these models effectively. Let's look at that next.

***

**Transition to Lab 2:**

Whether it's Phi-3 running locally or GPT-4 running in the cloud — both process your input the same foundational way: they break text into tokens before doing anything with it. Tokens affect cost, they affect context limits, and they can behave very differently depending on the language you write in. Let's see that in action.

***

## Lab 2 — Tokenization Demo

- **Time**: 15–25 min
- **Section**: Tokens and cost
- **Practical Outcome**: Participants can visually see how tokenization differs across languages and code, and understand cost implications.

### Spoken intro

Tokens are the atomic unit of everything in these systems. Not words — tokens. A token might be a full word, a word fragment, a punctuation mark, or a whitespace character. The model never sees your text as you wrote it. It sees a sequence of numeric IDs, one per token.
Why does this matter? Because:

- Context windows are measured in tokens, not words or characters
- API costs are billed per token
- Different languages tokenize very differently — Spanish, Japanese, or code with long identifiers can cost significantly more tokens than equivalent English

Let me show you this live.

### Demo inputs

Paste the following three inputs into a tokenizer (use https://www.claudetokenizer.com, the Gemini tokenizer docs, or VS Code Copilot token counter):

```bash
Hello, I need help creating a monthly budget.
Hola, necesito ayuda para crear un presupuesto mensual.
function calculateMonthlyBudget(income, fixedExpenses, variableExpenses) { return income - fixedExpenses - variableExpenses; }
```

Count the tokens for each. The English sentence and the Spanish sentence express the same idea. But the Spanish one will typically cost more tokens — because the tokenizer was trained predominantly on English text, so Spanish words are often split into smaller sub-word pieces.

The code example is especially interesting. calculateMonthlyBudget is a single identifier — but it may tokenize into several pieces. Multiply that across a large codebase and you start to understand why pasting entire files into a prompt is expensive.

### Teaching point

Tokens are not words. Spanish, code, symbols, JSON, logs, and long identifiers can behave differently across tokenizers. Before you design a prompt-heavy feature, it's worth estimating token counts — not as a nice-to-have, but as a cost and feasibility check.

***
**Transition to Lab 3:**

Now you understand what tokens are. The next question is: how many can the model see at once? That's what the context window controls — and it's the single biggest source of "the model ignored my file" complaints. Let's see exactly why.
***

## Lab 3 — Context Window and Memory Limit

- **Time**: 25–35 min
- **Section**: Context limits and precision
- **Practical Outcome**: Participants can observe the difference in model precision based on what is — and isn't — in context.

### Spoken intro

The context window is not memory. The model does not "remember" the file you showed it three messages ago. It only has access to what's inside the current prompt — the text, the attached files, the selected code, and recent conversation turns. When a conversation grows long enough, earlier content silently falls out. The model doesn't tell you. It just starts guessing.

This is one of the most dangerous failure modes in practice: the model sounds just as confident when it's guessing as when it has the actual data. Let's demonstrate that.

### Demonstration sequence

Round 1 — No file attached:

```bash
What does the BudgetCalculator class do? What methods does it have?
```

Copilot will give a generic, plausible-sounding answer. Write it down. Notice that it lists method names — methods that may not exist in your actual code. This is not a bug. This is the model doing exactly what it was designed to do: predict the most likely continuation based on the word "BudgetCalculator." It has no access to your code. It's completing from training data.

Round 2 — Attach `BudgetCalculator.java` only:

```bash
What does the BudgetCalculator class do? What methods does it have?
```

Round 3 — Attach `BudgetCalculator.java` + `sample_budget.csv` + `multilingual_snippets.md`:

```bash
What does the BudgetCalculator class do? What methods does it have?
```

The answer may get longer, but it won't get more accurate about the Java class. The irrelevant files dilute the signal. More context is not always better context.

Context sources in VS Code Copilot

|Source|When included|
|--|--|
|Your chat message|Always|
|Active open file|Partial, when `@workspace` not used|
|Files attached with `#file`|Explicitly, in full|
|Selected text in editor|When you have a selection|
|`@workspace` index|Summarized, not full files|
|Conversation history|Recent turns only — older turns silently drop out|

### Teaching point

Context is not free. Every token you add to the prompt is a token that could have been used for reasoning. Precision beats volume. Attach the minimum set of files needed to answer the question — not everything.

***
**Transition to Lab 4:**

So we've seen that the model guesses when it doesn't have the right context. But the dangerous part isn't that it guesses — it's that it guesses confidently, with the same tone as when it's correct. That's what hallucination looks like in practice. And that's exactly what we're going to surface next.

***

## Lab 4 — Hallucination Reduction

- **Time**: 35–45 min
- **Section**: Confidence vs correctness
- **Practical Outcome**: Participants directly observe a hallucination, then see how providing context eliminates it, without changing the model.

### Spoken intro

Hallucination is a loaded word. It implies the model is doing something wrong. But from the model's perspective, it's doing exactly what it was trained to do: complete the sequence with the most statistically likely tokens. When the model says "the emergency fund threshold is 6 months" — that's a very reasonable completion for a question about budget calculators. It's just not what your code says.

The correct mental model is: absence of information in context leads to completion from training data. Your job as an AI practitioner is to make sure the right information is in context before you ask a question that depends on it.

Let's make this concrete.

### Step 1 — Ask with NO file in context (close all tabs, no file attached)

Ask Copilot each question and write the answers down:

Question A — thresholds:

```
In the BudgetCalculator class, what savings rate percentage marks the boundary
between AT_RISK and STABLE? What is the threshold for EXCELLENT?
```

>>Expected: the model gives plausible finance-textbook numbers like "10% for stable, 20% for excellent." Actual values in the code: 5% (AT_RISK→STABLE), 25% (HEALTHY→EXCELLENT).

Question B — iteration count:

```
How many monthly periods does calculateAnnualProjection compound over?
```

>>Expected: "12 periods — one per month over a year." Actual: the loop runs 13 times due to i <= 12.

Question C — emergency fund:

```
According to the BudgetCalculator implementation, how many months of expenses
should an emergency fund cover?
```

>>Expected: "3–6 months" or "6 months" (industry rule of thumb). Actual constant: EMERGENCY_FUND_MONTHS = 3.

### Step 2 — Attach the file, ask the same questions

Attach `src/BudgetCalculator.java` via the `#file` button and re-ask all three questions. The model will now cite exact line numbers and correct values.

### Teaching point

Say this out loud:

*"The model did not learn anything. The code did not change. The only thing that changed was what was inside the context window. This is why RAG and explicit file context exist — not to make the model smarter, but to give it the right information instead of letting it guess."*

### Step 3 — Run the tests to verify

```bash
bashjavac -d out src/BudgetCalculator.java test/BudgetCalculatorTest.java
java -cp out BudgetCalculatorTest
```

Point out the tests that target the off-by-one and the private constants. These tests were specifically designed to fail if someone uses the hallucinated value to "fix" the bug. A correct fix requires reading the actual code — which is exactly the habit we want to enforce.

***

**Transition to Lab 5:**

The hallucination lab shows us that the model needs the right information to give correct answers. But in practice, you can't always attach a single Java file. What if the answer lives across dozens of documents? What if the question requires knowing current company policy? That's where RAG and fine-tuning come in — and they solve very different problems.
***

## Lab 5 — RAG vs Fine-Tuning

- **Time**: 45–52 min
- **Section**: Retrieval vs behavioral training
- **Practical Outcome**: Participants understand when to reach for RAG vs fine-tuning, and can recognize the difference in an enterprise scenario.

### Spoken intro

There are two common ways to improve model outputs when out-of-the-box responses aren't good enough: RAG and fine-tuning. These are often confused, but they solve completely different problems.

RAG — Retrieval-Augmented Generation — is about giving the model the right documents at query time. You retrieve the relevant content and inject it into the context window before the model answers. The model itself doesn't change. You're just curating what it sees.

Fine-tuning is about changing the model's weights through additional training. You're teaching it a pattern — a consistent behavior, a tone, a format — that you want it to reproduce reliably, without needing to specify it every time in the prompt.

When should you use each?

|Need|Better fit|
|--|--|
|Answer using current internal documents|RAG|
|Enforce company-specific answer format or tone|Fine-tuning or instructions|
|Use fresh policies, tickets, docs, release notes|RAG|
|Teach the model a stable repeated pattern|Fine-tuning|
|Best enterprise pattern|RAG + instructions + evals; fine-tune only when repeated behavior is hard to prompt|

Exercise prompt

Ask Copilot or Claude:

```
You are answering using only the pasted policy excerpt below.

1. Answer the user question.
2. Quote the exact section used.
3. If the policy does not answer it, say "Not found in provided context."

Policy excerpt:
[paste a short internal-safe sample policy]

Question:
Can contractors access production data?
```

Observe how constraining the source changes the answer quality and auditability. This is the core principle behind RAG: not making the model smarter, but making it answerable and traceable.

### Teaching point

RAG is your first tool in most enterprise scenarios — it's simpler to implement, doesn't require ML expertise, keeps the model current without retraining, and gives you an audit trail. Fine-tune only when you have a stable, repeatable behavioral pattern that's genuinely hard to achieve through instructions alone.

***
**Transition to Lab 6:**
RAG and fine-tuning help us with what the model knows and how it responds. But what about what it does? We've been talking to the model like it's an answering machine. An agent is something different — it's a model that takes multiple steps, uses tools, and acts on your behalf. Let's build a minimal one right now.
***

## Lab 6 — Agentic Copilot Workflow

- **Time**: 52–60 min
- **Section**: Agents and structured workflows
- **Practical Outcome**: Participants see how to structure reusable, constrained agentic workflows using Copilot's custom instructions and prompt files.

### Spoken intro

An agent is not magic. Strip away the marketing and what you have is: a model, plus instructions, plus tools, plus a goal, plus a loop. The model plans a step, executes a tool, observes the result, and decides the next step. The loop continues until the goal is met or it's told to stop.

The risk with agents is not that they're powerful — it's that they can take irreversible actions confidently when they're wrong. A hallucination that produces a bad answer is annoying. A hallucination that deletes the wrong files or commits broken code is a different problem.

The way we manage that risk is through structure: clear instructions, constrained scope, required verification steps, and human review. Let's build that structure now in VS Code.

### Setup files

Create these files in your project:

`.github/copilot-instructions.md` — Sets baseline behavior for Copilot across the workspace:

```markdown
# Project AI instructions

Always:

- Explain assumptions before changing code.
- Prefer small, reviewable changes.
- Do not invent APIs or dependencies.
- Run or suggest tests after changes.
- If information is missing, ask for the specific file or command needed.
```

These instructions tell Copilot how to behave every time it acts in this project. Think of them as the rules of engagement for your AI collaborator.

`.github/prompts/review-code.prompt.md` — A reusable code review prompt:

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

`.github/prompts/create-tests.prompt.md` — A reusable test generation prompt:

```markdown
# Test generation prompt

Given the selected code:
1. Identify all boundary conditions and edge cases.
2. Generate test cases for each.
3. Use the existing test style and structure in this project.
4. Flag any behavior that appears to be a defect rather than a design decision.
```

Demo sequence

Use Copilot Agent mode or Copilot Chat with these prompt files to:

1. Run the code review prompt on BudgetCalculator.java
2. Ask it to generate tests using the test generation prompt
3. Point to the off-by-one it flags — or the thresholds it gets wrong without the file attached

Show that the prompt file creates a repeatable, structured workflow. You're not asking the model to improvise — you're giving it a job description with explicit output requirements.

### Teaching point

An agent is only as reliable as the structure around it. Prompt files give you reusability. Custom instructions give you constraints. Test verification gives you a check on correctness. Human review gives you accountability. None of these are optional in a production context.

The practical takeaway from everything today is this: do not ask AI to be correct by hope. Give it context, constrain the source, ask for stated assumptions, validate with tools, and keep humans in the review loop.

## Closing

- Time: 60 min

Let's close with the four mental models we opened with. Every lab today was an instance of one of these:

1. AI is probabilistic — We saw this in the hallucination lab. Same question, different answers, depending on what was in context. No guarantees.
2. AI is context-bound — We saw this in the tokenization and context window labs. The model can only use what you give it. Everything else is invisible.
3. AI is token-priced — We saw this in the tokenization demo. Spanish costs more tokens than English. Code identifiers cost more than prose. Every token is a decision.
4. AI requires validation — We saw this in every lab. Tests caught the hallucinated fix. Context surfaced the actual threshold. Human review kept the agent honest.

The gap between "AI that sometimes works" and "AI you can rely on in production" is entirely in how you structure context, constrain scope, and validate outputs. Today you've seen all three.

Thank you — questions?
