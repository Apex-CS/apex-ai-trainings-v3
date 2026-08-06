package com.owasp.aiassistant.guardrails;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.owasp.aiassistant.config.GuardrailProperties;
import com.owasp.aiassistant.config.OutputJudgeProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class OutputJudgeServiceTest {

    private GuardrailService guardrailService;
    private ChatModel judgeChatModel;
    private OutputJudgeService outputJudgeService;

    @BeforeEach
    void setUp() {
        GuardrailProperties guardrailProperties = new GuardrailProperties();
        guardrailProperties.setEnabled(true);

        guardrailService = new GuardrailService(guardrailProperties, List.of(policySource(List.of(
                new GuardrailPolicy(
                        "output-policy",
                        "output policy",
                        GuardrailType.OUTPUT,
                        GuardrailEnforcement.SOFT,
                        "Never fabricate financial figures.",
                        "test"),
                new GuardrailPolicy(
                        "judge-rubric",
                        "judge rubric",
                        GuardrailType.JUDGE,
                        GuardrailEnforcement.SOFT,
                        "Return JSON with pass and violations fields.",
                        "test")))));
        guardrailService.reload();

        judgeChatModel = mock(ChatModel.class);

        OutputJudgeProperties outputJudgeProperties = new OutputJudgeProperties();
        outputJudgeProperties.setEnabled(true);

        outputJudgeService = new OutputJudgeService(
                judgeChatModel,
                guardrailService,
                guardrailProperties,
                outputJudgeProperties,
                new ObjectMapper());
    }

    @Test
    void evaluateReturnsPassedWhenJudgeApproves() {
        when(judgeChatModel.call(any(Prompt.class))).thenReturn(judgeResponse("""
                {
                  "pass": true,
                  "violations": [],
                  "summary": "Compliant"
                }
                """));

        OutputJudgeEvaluation evaluation = outputJudgeService.evaluate(
                "What were Q3 sales?",
                "Q3 sales were $1.2M per the database.");

        assertTrue(evaluation.passed());
        assertFalse(evaluation.unavailable());
    }

    @Test
    void evaluateReturnsFailedWhenJudgeRejects() {
        when(judgeChatModel.call(any(Prompt.class))).thenReturn(judgeResponse("""
                {
                  "pass": false,
                  "violations": ["Fabricated financial figure"],
                  "summary": "Unsupported claim"
                }
                """));

        OutputJudgeEvaluation evaluation = outputJudgeService.evaluate(
                "What were Q3 sales?",
                "Q3 sales were $99B.");

        assertFalse(evaluation.passed());
        assertTrue(evaluation.violations().contains("Fabricated financial figure"));
    }

    @Test
    void buildRegenerationGuidanceIncludesViolations() {
        String guidance = outputJudgeService.buildRegenerationGuidance(OutputJudgeEvaluation.failed(
                List.of("Missing citation"),
                "Citations required"));

        assertTrue(guidance.contains("Missing citation"));
        assertTrue(guidance.contains("Citations required"));
    }

    private static ChatResponse judgeResponse(String json) {
        return new ChatResponse(List.of(new Generation(new AssistantMessage(json))));
    }

    private static GuardrailPolicySource policySource(List<GuardrailPolicy> policies) {
        return new GuardrailPolicySource() {
            @Override
            public String getSourceName() {
                return "test";
            }

            @Override
            public List<GuardrailPolicy> loadPolicies() {
                return policies;
            }
        };
    }
}
