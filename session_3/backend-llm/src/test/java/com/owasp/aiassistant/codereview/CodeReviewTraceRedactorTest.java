package com.owasp.aiassistant.codereview;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.owasp.aiassistant.dto.CodeAttachment;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class CodeReviewTraceRedactorTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void redactsBase64PayloadInTraceState() throws Exception {
        CodeAttachment attachment = new CodeAttachment(
                "app.py",
                "text/x-python",
                "base64",
                "cHJpbnQoJ2hlbGxvJyk=");
        String json = objectMapper.writeValueAsString(attachment);

        Object redacted = CodeReviewTraceRedactor.redact(json, objectMapper);

        assertTrue(redacted.toString().contains("app.py"));
        assertTrue(redacted.toString().contains("[redacted"));
        assertTrue(!redacted.toString().contains("cHJpbnQoJ2hlbGxvJyk="));
    }
}
