package com.owasp.aiassistant.guardrails;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OutputJudgeResponseParserTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void parsesPassingVerdict() {
        OutputJudgeEvaluation evaluation = OutputJudgeResponseParser.parse(
                """
                        {
                          "pass": true,
                          "violations": [],
                          "summary": "Compliant response"
                        }
                        """,
                objectMapper);

        assertTrue(evaluation.passed());
        assertFalse(evaluation.unavailable());
        assertEquals("Compliant response", evaluation.summary());
    }

    @Test
    void parsesFailingVerdictWithViolations() {
        OutputJudgeEvaluation evaluation = OutputJudgeResponseParser.parse(
                """
                        {
                          "pass": false,
                          "violations": ["Fabricated revenue figure"],
                          "summary": "Unsupported financial claim"
                        }
                        """,
                objectMapper);

        assertFalse(evaluation.passed());
        assertEquals(1, evaluation.violations().size());
        assertEquals("Fabricated revenue figure", evaluation.violations().getFirst());
    }

    @Test
    void parsesJsonInsideMarkdownFence() {
        OutputJudgeEvaluation evaluation = OutputJudgeResponseParser.parse(
                """
                        ```json
                        {
                          "pass": true,
                          "violations": [],
                          "summary": "OK"
                        }
                        ```
                        """,
                objectMapper);

        assertTrue(evaluation.passed());
    }

    @Test
    void treatsInvalidJsonAsUnavailable() {
        OutputJudgeEvaluation evaluation = OutputJudgeResponseParser.parse("not json", objectMapper);

        assertTrue(evaluation.unavailable());
    }
}
