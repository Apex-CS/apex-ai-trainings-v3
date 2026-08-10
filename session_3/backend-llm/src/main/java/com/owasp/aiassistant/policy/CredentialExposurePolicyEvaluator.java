package com.owasp.aiassistant.policy;

import com.owasp.aiassistant.dto.CodeAttachment;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.Enumeration;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

@Component
public class CredentialExposurePolicyEvaluator {

    private static final String BLOCK_REASON = """
            I can't process messages or attachments that contain application credentials, tokens, or secrets. \
            Please remove sensitive values and try again.""";

    private static final Pattern PROPERTY_ASSIGNMENT = Pattern.compile(
            "^\\s*([A-Za-z_][\\w.-]*)\\s*([=:])\\s*(.+?)\\s*$");

    private static final Pattern JWT = Pattern.compile(
            "eyJ[A-Za-z0-9_-]+\\.[A-Za-z0-9_-]+\\.[A-Za-z0-9_-]+");

    private static final Pattern SENSITIVE_KEY = Pattern.compile(
            "(?i)(?:^|[_\\-.])(?:password|passwd|pwd|secret|api[_-]?key|apikey|access[_-]?key|"
                    + "private[_-]?key|client[_-]?secret|auth[_-]?token|bearer[_-]?token|credential|"
                    + "databricks[_-]?token|connection[_-]?string|db[_-]?password|database[_-]?url)"
                    + "(?:$|[_\\-.])|(?:^|[_\\-.])token(?:$|[_\\-.])");

    private static final Pattern PLACEHOLDER_VALUE = Pattern.compile(
            "(?i)^(?:\\$\\{[^}]+}|<%[^%]+%>|<[^>]+>|changeme|placeholder|xxx+|your[-_]?\\w+|"
                    + "todo|fixme|none|null|true|false|\\*+)$");

    public CredentialExposurePolicyEvaluation evaluate(String userMessage, CodeAttachment attachment) {
        Optional<String> messageHit = scanText(userMessage, "message");
        if (messageHit.isPresent()) {
            return CredentialExposurePolicyEvaluation.blocked(
                    "User provided application credentials or secrets in chat input (" + messageHit.get() + ")",
                    BLOCK_REASON);
        }

        if (attachment != null) {
            Optional<String> attachmentHit = scanAttachment(attachment);
            if (attachmentHit.isPresent()) {
                return CredentialExposurePolicyEvaluation.blocked(
                        "User provided application credentials or secrets in attachment ("
                                + attachmentHit.get() + ")",
                        BLOCK_REASON);
            }
        }

        return CredentialExposurePolicyEvaluation.allowed();
    }

    Optional<String> scanText(String text, String sourceLabel) {
        if (text == null || text.isBlank()) {
            return Optional.empty();
        }

        Matcher jwtMatcher = JWT.matcher(text);
        if (jwtMatcher.find()) {
            return Optional.of(sourceLabel + ": JWT token");
        }

        for (String line : text.split("\\R")) {
            Optional<String> propertyHit = scanPropertyLine(line);
            if (propertyHit.isPresent()) {
                return Optional.of(sourceLabel + ": " + propertyHit.get());
            }
        }

        return Optional.empty();
    }

    private Optional<String> scanAttachment(CodeAttachment attachment) {
        if (!"base64".equalsIgnoreCase(attachment.encoding())) {
            return Optional.empty();
        }

        byte[] bytes;
        try {
            bytes = Base64.getDecoder().decode(attachment.data());
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }

        String filename = attachment.filename() == null ? "" : attachment.filename();
        if (isZip(filename, attachment.contentType())) {
            return scanZip(bytes);
        }

        return scanText(decodeText(bytes), filename);
    }

    private Optional<String> scanZip(byte[] zipBytes) {
        Path tempZip = null;
        try {
            tempZip = Files.createTempFile("credential-scan-", ".zip");
            Files.write(tempZip, zipBytes);

            try (ZipFile zipFile = new ZipFile(tempZip.toFile())) {
                Enumeration<? extends ZipEntry> entries = zipFile.entries();
                while (entries.hasMoreElements()) {
                    ZipEntry entry = entries.nextElement();
                    if (entry.isDirectory()) {
                        continue;
                    }

                    String path = normalizeZipPath(entry.getName());
                    if (path.contains("..") || path.startsWith("/")) {
                        continue;
                    }

                    try (InputStream inputStream = zipFile.getInputStream(entry)) {
                        byte[] entryBytes = readEntryBytes(inputStream);
                        if (containsNullByte(entryBytes)) {
                            continue;
                        }
                        Optional<String> hit = scanText(
                                new String(entryBytes, StandardCharsets.UTF_8),
                                path);
                        if (hit.isPresent()) {
                            return hit;
                        }
                    }
                }
            }
        } catch (IOException | IllegalArgumentException e) {
            return Optional.empty();
        } finally {
            if (tempZip != null) {
                try {
                    Files.deleteIfExists(tempZip);
                } catch (IOException ignored) {
                    // best-effort cleanup
                }
            }
        }

        return Optional.empty();
    }

    private static Optional<String> scanPropertyLine(String line) {
        String trimmed = line.trim();
        if (trimmed.isEmpty() || isCommentLine(trimmed)) {
            return Optional.empty();
        }

        Matcher matcher = PROPERTY_ASSIGNMENT.matcher(trimmed);
        if (!matcher.matches()) {
            return Optional.empty();
        }

        String key = matcher.group(1);
        String value = stripQuotes(matcher.group(3));
        if (!isSensitiveKey(key) || !looksLikeSecretValue(value)) {
            return Optional.empty();
        }

        return Optional.of("property `" + key + "`");
    }

    private static boolean isCommentLine(String line) {
        return line.startsWith("#")
                || line.startsWith("//")
                || line.startsWith(";");
    }

    private static boolean isSensitiveKey(String key) {
        String normalized = key.toLowerCase(Locale.ROOT);
        if (normalized.contains("max-token")
                || normalized.contains("max_token")
                || normalized.contains("tokenizer")
                || normalized.endsWith("_port")
                || normalized.endsWith("-port")
                || normalized.equals("port")
                || normalized.endsWith("_host")
                || normalized.endsWith("-host")
                || normalized.equals("host")) {
            return false;
        }
        return SENSITIVE_KEY.matcher(key).find();
    }

    private static boolean looksLikeSecretValue(String value) {
        if (value == null || value.isBlank() || value.length() < 4) {
            return false;
        }
        return !PLACEHOLDER_VALUE.matcher(value).matches();
    }

    private static String stripQuotes(String value) {
        String trimmed = value.trim();
        if ((trimmed.startsWith("\"") && trimmed.endsWith("\""))
                || (trimmed.startsWith("'") && trimmed.endsWith("'"))) {
            return trimmed.substring(1, trimmed.length() - 1).trim();
        }
        return trimmed;
    }

    private static String decodeText(byte[] bytes) {
        return new String(bytes, StandardCharsets.UTF_8);
    }

    private static boolean containsNullByte(byte[] bytes) {
        for (byte b : bytes) {
            if (b == 0) {
                return true;
            }
        }
        return false;
    }

    private static byte[] readEntryBytes(InputStream inputStream) throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        byte[] chunk = new byte[8192];
        long totalRead = 0;
        int read;
        while ((read = inputStream.read(chunk)) >= 0) {
            totalRead += read;
            if (totalRead > 2L * 1024 * 1024) {
                throw new IllegalArgumentException("Zip entry is too large");
            }
            buffer.write(chunk, 0, read);
        }
        return buffer.toByteArray();
    }

    private static boolean isZip(String filename, String contentType) {
        String lowerName = filename == null ? "" : filename.toLowerCase(Locale.ROOT);
        return lowerName.endsWith(".zip")
                || (contentType != null && contentType.toLowerCase(Locale.ROOT).contains("zip"));
    }

    private static String normalizeZipPath(String entryName) {
        String normalized = entryName.replace('\\', '/');
        while (normalized.startsWith("./")) {
            normalized = normalized.substring(2);
        }
        return normalized;
    }
}
