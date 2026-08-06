package com.owasp.aiassistant.guardrails;

import java.util.List;

public record OutputJudgeEvaluation(
        boolean passed,
        List<String> violations,
        String summary,
        boolean unavailable) {

    public static OutputJudgeEvaluation passed(String summary) {
        return new OutputJudgeEvaluation(true, List.of(), summary, false);
    }

    public static OutputJudgeEvaluation failed(List<String> violations, String summary) {
        return new OutputJudgeEvaluation(false, List.copyOf(violations), summary, false);
    }

    public static OutputJudgeEvaluation unavailable(String summary) {
        return new OutputJudgeEvaluation(true, List.of(), summary, true);
    }

    public static OutputJudgeEvaluation unavailableBlocked(String summary) {
        return new OutputJudgeEvaluation(false, List.of(), summary, true);
    }
}
