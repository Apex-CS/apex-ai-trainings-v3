package com.owasp.aiassistant.codereview;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

public final class CodeReviewTraceRedactor {

    private CodeReviewTraceRedactor() {
    }

    public static Object redact(Object value, ObjectMapper objectMapper) {
        if (!(value instanceof String json)) {
            return value;
        }

        try {
            JsonNode node = objectMapper.readTree(json);
            if (!node.isObject() || !node.has("data")) {
                return value;
            }

            ObjectNode copy = ((ObjectNode) node).deepCopy();
            String data = copy.path("data").asText("");
            copy.put("data", "[redacted, " + data.length() + " base64 chars]");
            return objectMapper.writeValueAsString(copy);
        } catch (Exception ignored) {
            if (json.length() > 200) {
                return json.substring(0, 200) + "... [redacted]";
            }
            return value;
        }
    }
}
