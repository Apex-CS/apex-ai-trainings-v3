package com.acme.aitraining.review;

import java.util.List;

/**
 * STEP 2 — Structured output target.
 * Spring AI generates a JSON schema from this record, injects it into the prompt,
 * and parses the model's JSON reply back into a typed instance.
 */
public record CodeReview(
        String summary,
        Severity overallSeverity,
        List<Issue> issues,
        int qualityScore // 0..100
) {
    public enum Severity { LOW, MEDIUM, HIGH, CRITICAL }

    public record Issue(String title, Severity severity, String explanation, String suggestedFix) {}
}
