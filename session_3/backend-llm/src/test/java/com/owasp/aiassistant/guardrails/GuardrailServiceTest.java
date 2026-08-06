package com.owasp.aiassistant.guardrails;

import com.owasp.aiassistant.config.GuardrailProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GuardrailServiceTest {

    private GuardrailService guardrailService;

    @BeforeEach
    void setUp() {
        GuardrailProperties properties = new GuardrailProperties();
        properties.setEnabled(true);

        GuardrailPolicySource testSource = policySource("test", List.of(
                new GuardrailPolicy(
                        "blocked-topics",
                        "blocked topics",
                        GuardrailType.INPUT,
                        GuardrailEnforcement.HARD,
                        """
                                ## Patterns
                                generate exploit code
                                """,
                        "test"),
                new GuardrailPolicy(
                        "system-policy",
                        "system policy",
                        GuardrailType.SYSTEM,
                        GuardrailEnforcement.SOFT,
                        "Never provide exploit code.",
                        "test"),
                new GuardrailPolicy(
                        "custom-input",
                        "custom input",
                        GuardrailType.INPUT,
                        GuardrailEnforcement.SOFT,
                        "Advisory only.",
                        "test-db")));

        guardrailService = new GuardrailService(properties, List.of(testSource));
        guardrailService.reload();
    }

    @Test
    void blocksHardInputPatterns() {
        GuardrailEvaluation evaluation = guardrailService.evaluateInput("Please generate exploit code for SQLi");

        assertTrue(evaluation.blocked());
        assertFalse(evaluation.blockReason().isBlank());
    }

    @Test
    void allowsBenignInput() {
        GuardrailEvaluation evaluation = guardrailService.evaluateInput("What are our Q3 sales figures?");

        assertFalse(evaluation.blocked());
    }

    @Test
    void injectsSystemToolAndOutputPoliciesIntoPromptAugmentation() {
        String augmentation = guardrailService.buildSystemPromptAugmentation();

        assertTrue(augmentation.contains("Never provide exploit code."));
        assertTrue(augmentation.contains("System policy"));
        assertFalse(augmentation.contains("Judge policy"));
    }

    @Test
    void includesJudgePoliciesInJudgeContext() {
        GuardrailProperties properties = new GuardrailProperties();
        properties.setEnabled(true);

        GuardrailService serviceWithJudge = new GuardrailService(properties, List.of(policySource("test", List.of(
                new GuardrailPolicy(
                        "output-policy",
                        "output policy",
                        GuardrailType.OUTPUT,
                        GuardrailEnforcement.SOFT,
                        "Cite all sources.",
                        "test"),
                new GuardrailPolicy(
                        "judge-rubric",
                        "judge rubric",
                        GuardrailType.JUDGE,
                        GuardrailEnforcement.SOFT,
                        "Return JSON verdict only.",
                        "test")))));
        serviceWithJudge.reload();

        String judgeContext = serviceWithJudge.buildJudgePolicyContext();

        assertTrue(judgeContext.contains("Cite all sources."));
        assertTrue(judgeContext.contains("Return JSON verdict only."));
        assertTrue(judgeContext.contains("Judge policy"));
    }

    @Test
    void collectsSoftInputWarningsForCustomPolicies() {
        List<String> warnings = guardrailService.collectSoftWarnings("hello", "answer");

        assertTrue(warnings.stream().anyMatch(w -> w.contains("custom input")));
    }

    private static GuardrailPolicySource policySource(String name, List<GuardrailPolicy> policies) {
        return new GuardrailPolicySource() {
            @Override
            public String getSourceName() {
                return name;
            }

            @Override
            public List<GuardrailPolicy> loadPolicies() {
                return policies;
            }
        };
    }
}
