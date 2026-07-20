package com.acme.lc4j;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

/**
 * THE signature feature of LangChain4j: AI Services.
 * You declare an interface; the library generates the implementation:
 * prompt templating + structured output parsing + memory + tools.
 *
 * Analogy for the talk: "Spring Data repositories / Retrofit... for LLMs."
 */
public interface ReviewAssistant {

    @SystemMessage("You are a strict senior Java code reviewer. Be concise.")
    @UserMessage("Review this Java code and score it 0-100:\n\n{{code}}")
    CodeReview review(@V("code") String code);

    @SystemMessage("You write conventional git commit messages (feat|fix|chore...).")
    @UserMessage("Write a one-line commit message for this diff:\n\n{{diff}}")
    String commitMessage(@V("diff") String diff);
}
