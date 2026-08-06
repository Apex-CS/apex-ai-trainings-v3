package com.owasp.aiassistant.guardrails;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class OutputJudgeResponseParser {

    private static final Pattern JSON_BLOCK = Pattern.compile("```(?:json)?\\s*([\\s\\S]*?)```", Pattern.CASE_INSENSITIVE);

    private OutputJudgeResponseParser() {
    }

    static OutputJudgeEvaluation parse(String rawResponse, ObjectMapper objectMapper) {
        if (rawResponse == null || rawResponse.isBlank()) {
            return OutputJudgeEvaluation.unavailable("Judge returned an empty response");
        }

        String json = extractJson(rawResponse.trim());
        try {
            JsonNode root = objectMapper.readTree(json);
            boolean pass = root.path("pass").asBoolean(false);
            String summary = root.path("summary").asText("No summary provided");

            List<String> violations = new ArrayList<>();
            JsonNode violationsNode = root.path("violations");
            if (violationsNode.isArray()) {
                violationsNode.forEach(node -> {
                    String violation = node.asText("").trim();
                    if (!violation.isEmpty()) {
                        violations.add(violation);
                    }
                });
            }

            if (pass) {
                return OutputJudgeEvaluation.passed(summary);
            }
            if (violations.isEmpty()) {
                violations.add(summary);
            }
            return OutputJudgeEvaluation.failed(violations, summary);
        } catch (Exception e) {
            return OutputJudgeEvaluation.unavailable("Failed to parse judge response: " + e.getMessage());
        }
    }

    private static String extractJson(String rawResponse) {
        Matcher matcher = JSON_BLOCK.matcher(rawResponse);
        if (matcher.find()) {
            return matcher.group(1).trim();
        }
        return rawResponse;
    }
}
