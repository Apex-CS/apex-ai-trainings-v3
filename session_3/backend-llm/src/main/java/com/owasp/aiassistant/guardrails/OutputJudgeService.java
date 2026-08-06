package com.owasp.aiassistant.guardrails;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.owasp.aiassistant.config.GuardrailProperties;
import com.owasp.aiassistant.config.OutputJudgeProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@ConditionalOnProperty(name = "app.guardrails.output-judge.enabled", havingValue = "true", matchIfMissing = true)
public class OutputJudgeService {

    private static final Logger log = LoggerFactory.getLogger(OutputJudgeService.class);

    private static final String JUDGE_SYSTEM_PROMPT = """
            You are an independent compliance judge for Example Company AI assistant responses.
            Your only job is to evaluate whether an assistant response complies with company policy.
            Do not answer the user's question. Do not rewrite the response.
            Return JSON only, matching the format described in the judge rubric.
            """;

    private static final String BLOCKED_RESPONSE_MESSAGE = """
            I can't provide that response because it doesn't meet Example Company's assistant policies. \
            Please rephrase your request or ask for help with a compliant alternative.""";

    private final ChatModel judgeChatModel;
    private final GuardrailService guardrailService;
    private final GuardrailProperties guardrailProperties;
    private final OutputJudgeProperties outputJudgeProperties;
    private final ObjectMapper objectMapper;

    public OutputJudgeService(
            @Qualifier("judgeChatModel") ChatModel judgeChatModel,
            GuardrailService guardrailService,
            GuardrailProperties guardrailProperties,
            OutputJudgeProperties outputJudgeProperties,
            ObjectMapper objectMapper) {
        this.judgeChatModel = judgeChatModel;
        this.guardrailService = guardrailService;
        this.guardrailProperties = guardrailProperties;
        this.outputJudgeProperties = outputJudgeProperties;
        this.objectMapper = objectMapper;
    }

    public boolean isEnabled() {
        return guardrailProperties.isEnabled() && outputJudgeProperties.isEnabled();
    }

    public int getMaxRetries() {
        return Math.max(0, outputJudgeProperties.getMaxRetries());
    }

    public GuardrailEnforcement getEnforcement() {
        return outputJudgeProperties.getEnforcement();
    }

    public String blockedResponseMessage() {
        return BLOCKED_RESPONSE_MESSAGE;
    }

    public OutputJudgeEvaluation evaluate(String userMessage, String assistantAnswer) {
        if (!isEnabled()) {
            return OutputJudgeEvaluation.passed("Output judge disabled");
        }
        if (assistantAnswer == null || assistantAnswer.isBlank()) {
            return OutputJudgeEvaluation.failed(
                    List.of("Empty assistant response"),
                    "Assistant response was empty");
        }

        String policyContext = guardrailService.buildJudgePolicyContext();
        String judgeUserPrompt = buildJudgeUserPrompt(userMessage, assistantAnswer, policyContext);

        try {
            ChatResponse response = judgeChatModel.call(new Prompt(List.of(
                    new SystemMessage(JUDGE_SYSTEM_PROMPT),
                    new UserMessage(judgeUserPrompt))));
            String rawVerdict = response.getResult().getOutput().getText();
            OutputJudgeEvaluation evaluation = OutputJudgeResponseParser.parse(rawVerdict, objectMapper);

            if (evaluation.unavailable()) {
                log.warn("Output judge returned an unparseable verdict: {}", evaluation.summary());
                return handleJudgeError(evaluation.summary());
            }

            if (!evaluation.passed()) {
                log.info("Output judge rejected response: {}", evaluation.violations());
            }
            return evaluation;
        } catch (Exception e) {
            log.warn("Output judge invocation failed: {}", e.getMessage());
            return handleJudgeError(e.getMessage());
        }
    }

    public String buildRegenerationGuidance(OutputJudgeEvaluation evaluation) {
        StringBuilder guidance = new StringBuilder();
        guidance.append("\n\nYour previous response was rejected by the compliance judge.\n");
        guidance.append("Regenerate a fully compliant answer.\n");
        if (evaluation.summary() != null && !evaluation.summary().isBlank()) {
            guidance.append("Judge summary: ").append(evaluation.summary().trim()).append("\n");
        }
        if (!evaluation.violations().isEmpty()) {
            guidance.append("Violations to fix:\n");
            for (String violation : evaluation.violations()) {
                guidance.append("- ").append(violation).append("\n");
            }
        }
        return guidance.toString();
    }

    private OutputJudgeEvaluation handleJudgeError(String reason) {
        if (outputJudgeProperties.isFailOpenOnError()) {
            return OutputJudgeEvaluation.unavailable(reason);
        }
        return OutputJudgeEvaluation.unavailableBlocked(
                "Response blocked because the output judge is unavailable: " + reason);
    }

    private static String buildJudgeUserPrompt(String userMessage, String assistantAnswer, String policyContext) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("# Company policies\n\n");
        if (policyContext == null || policyContext.isBlank()) {
            prompt.append("No policy context loaded.\n\n");
        } else {
            prompt.append(policyContext.trim()).append("\n\n");
        }

        prompt.append("# User message\n\n");
        prompt.append(userMessage != null ? userMessage.trim() : "").append("\n\n");
        prompt.append("# Assistant response to evaluate\n\n");
        prompt.append(assistantAnswer.trim()).append("\n\n");
        prompt.append("Evaluate the assistant response against the policies above. Return JSON only.");
        return prompt.toString();
    }
}
