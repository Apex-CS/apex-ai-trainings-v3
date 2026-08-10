package com.owasp.aiassistant.policy;

import com.owasp.aiassistant.dto.CodeAttachment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CredentialExposurePolicyEvaluatorTest {

    private CredentialExposurePolicyEvaluator evaluator;

    @BeforeEach
    void setUp() {
        evaluator = new CredentialExposurePolicyEvaluator();
    }

    @Test
    void blocksPropertyValueCredentialsInMessage() {
        CredentialExposurePolicyEvaluation evaluation = evaluator.evaluate(
                "Please use this config:\nAPI_KEY=sk-live-abcdef123456",
                null);

        assertTrue(evaluation.blocked());
    }

    @Test
    void blocksYamlStyleSecretsInMessage() {
        CredentialExposurePolicyEvaluation evaluation = evaluator.evaluate(
                "database:\n  password: super-secret-value",
                null);

        assertTrue(evaluation.blocked());
    }

    @Test
    void blocksJwtTokensInMessage() {
        CredentialExposurePolicyEvaluation evaluation = evaluator.evaluate(
                "Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJ0ZXN0In0.signature",
                null);

        assertTrue(evaluation.blocked());
    }

    @Test
    void allowsBenignConfigurationValues() {
        CredentialExposurePolicyEvaluation evaluation = evaluator.evaluate(
                "POSTGRES_HOST_PORT=5433\nmax-tokens: 4096",
                null);

        assertFalse(evaluation.blocked());
    }

    @Test
    void allowsPlaceholderSecretValues() {
        CredentialExposurePolicyEvaluation evaluation = evaluator.evaluate(
                "DB_PASSWORD=${POSTGRES_PASSWORD}",
                null);

        assertFalse(evaluation.blocked());
    }

    @Test
    void blocksSecretsInEnvAttachment() {
        CodeAttachment attachment = attachment(
                ".env",
                "text/plain",
                "DATABRICKS_TOKEN=databricks-secret-token-value\nPOSTGRES_HOST_PORT=5433");

        CredentialExposurePolicyEvaluation evaluation = evaluator.evaluate(null, attachment);

        assertTrue(evaluation.blocked());
    }

    @Test
    void blocksSecretsInsideZipAttachment() throws IOException {
        byte[] zipBytes = zipWithEntry(".env", "CLIENT_SECRET=top-secret-client-value");
        CodeAttachment attachment = new CodeAttachment(
                "project.zip",
                "application/zip",
                "base64",
                Base64.getEncoder().encodeToString(zipBytes));

        CredentialExposurePolicyEvaluation evaluation = evaluator.evaluate(null, attachment);

        assertTrue(evaluation.blocked());
    }

    @Test
    void allowsRegularCodeAttachment() {
        CodeAttachment attachment = attachment(
                "app.py",
                "text/x-python",
                "print('hello world')");

        CredentialExposurePolicyEvaluation evaluation = evaluator.evaluate(null, attachment);

        assertFalse(evaluation.blocked());
    }

    private static CodeAttachment attachment(String filename, String contentType, String source) {
        return new CodeAttachment(
                filename,
                contentType,
                "base64",
                Base64.getEncoder().encodeToString(source.getBytes(StandardCharsets.UTF_8)));
    }

    private static byte[] zipWithEntry(String path, String content) throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        try (ZipOutputStream zipOutputStream = new ZipOutputStream(buffer)) {
            zipOutputStream.putNextEntry(new ZipEntry(path));
            zipOutputStream.write(content.getBytes(StandardCharsets.UTF_8));
            zipOutputStream.closeEntry();
        }
        return buffer.toByteArray();
    }
}
