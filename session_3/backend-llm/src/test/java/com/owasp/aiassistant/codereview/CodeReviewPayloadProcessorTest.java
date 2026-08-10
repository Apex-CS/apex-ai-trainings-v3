package com.owasp.aiassistant.codereview;

import com.owasp.aiassistant.config.CodeReviewProperties;
import com.owasp.aiassistant.dto.CodeAttachment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CodeReviewPayloadProcessorTest {

    private CodeReviewPayloadProcessor processor;

    @BeforeEach
    void setUp() {
        CodeReviewProperties properties = new CodeReviewProperties();
        properties.setMaxUploadBytes(1024 * 1024);
        properties.setMaxUncompressedBytes(1024 * 1024);
        properties.setMaxFiles(50);
        properties.setMaxTextChars(50_000);
        processor = new CodeReviewPayloadProcessor(properties);
    }

    @Test
    void preparesSinglePythonFile() {
        String source = "print('hello')\n";
        CodeAttachment attachment = attachment("app.py", "text/x-python", source);

        String prepared = processor.prepareForReview(attachment, "all");

        assertTrue(prepared.contains("Source: app.py"));
        assertTrue(prepared.contains("=== PROJECT STRUCTURE ==="));
        assertTrue(prepared.contains("app.py"));
        assertTrue(prepared.contains("print('hello')"));
    }

    @Test
    void expandsZipArchiveWithDirectoryTree() throws Exception {
        byte[] zipBytes = zipOf(
                entry("src/main/App.java", "class App {}"),
                entry("src/main/AppTest.java", "class AppTest {}"),
                entry("node_modules/pkg/index.js", "ignored"));
        CodeAttachment attachment = new CodeAttachment(
                "project.zip",
                "application/zip",
                "base64",
                Base64.getEncoder().encodeToString(zipBytes));

        String prepared = processor.prepareForReview(attachment, "auth");

        assertTrue(prepared.contains("Review focus: auth"));
        assertTrue(prepared.contains("src/"));
        assertTrue(prepared.contains("class App {}"));
        assertTrue(prepared.contains("node_modules/pkg/index.js (excluded path)"));
        assertTrue(prepared.contains("=== FILE CONTENTS ==="));
    }

    @Test
    void skipsBinaryFilesInsideZipArchive() throws Exception {
        byte[] zipBytes = zipWithBinaryEntry("good.py", "print('ok')", "corrupt.py", new byte[]{0x00, 0x01, (byte) 0xFF});
        CodeAttachment attachment = new CodeAttachment(
                "project.zip",
                "application/zip",
                "base64",
                Base64.getEncoder().encodeToString(zipBytes));

        String prepared = processor.prepareForReview(attachment, "all");

        assertTrue(prepared.contains("print('ok')"));
        assertTrue(prepared.contains("corrupt.py (binary content)"));
    }

    @Test
    void readsZipWithStoredEntriesAndDataDescriptor() {
        // macOS `zip -0` produces STORED entries with data descriptors, which ZipInputStream cannot read.
        byte[] zipBytes = hexToBytes(
                "504b03040a0000000000787c055d34b2b7e00c0000000c00000006001c006170702e70795554090003"
                        + "a49e736aa59e736a75780b000104f601000004140000007072696e7428276f6b27290a504b0102"
                        + "1e030a0000000000787c055d34b2b7e00c0000000c000000060018000000000000000000a481"
                        + "000000006170702e70795554050003a49e736a75780b000104f60100000414000000504b0506"
                        + "00000000010001004c0000004c0000000000");
        CodeAttachment attachment = new CodeAttachment(
                "project.zip",
                "application/zip",
                "base64",
                Base64.getEncoder().encodeToString(zipBytes));

        String prepared = processor.prepareForReview(attachment, "all");

        assertTrue(prepared.contains("app.py"));
        assertTrue(prepared.contains("print('ok')"));
    }

    @Test
    void rejectsUnsafeZipPaths() throws Exception {
        byte[] zipBytes = zipOf(entry("../evil.py", "bad"));
        CodeAttachment attachment = new CodeAttachment(
                "unsafe.zip",
                "application/zip",
                "base64",
                Base64.getEncoder().encodeToString(zipBytes));

        assertThrows(IllegalArgumentException.class, () -> processor.prepareForReview(attachment, "all"));
    }

    @Test
    void buildDirectoryTree_formatsNestedPaths() {
        String tree = CodeReviewPayloadProcessor.buildDirectoryTree(List.of(
                "src/main/App.java",
                "src/test/AppTest.java",
                "README.md"));

        assertTrue(tree.contains("src"));
        assertTrue(tree.contains("App.java"));
        assertTrue(tree.contains("README.md"));
    }

    private static CodeAttachment attachment(String filename, String contentType, String source) {
        return new CodeAttachment(
                filename,
                contentType,
                "base64",
                Base64.getEncoder().encodeToString(source.getBytes(StandardCharsets.UTF_8)));
    }

    private static ZipEntryData entry(String path, String content) {
        return new ZipEntryData(path, content);
    }

    private static byte[] zipWithBinaryEntry(
            String textPath, String textContent, String binaryPath, byte[] binaryContent) throws Exception {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        try (ZipOutputStream zipOutputStream = new ZipOutputStream(buffer)) {
            zipOutputStream.putNextEntry(new ZipEntry(textPath));
            zipOutputStream.write(textContent.getBytes(StandardCharsets.UTF_8));
            zipOutputStream.closeEntry();
            zipOutputStream.putNextEntry(new ZipEntry(binaryPath));
            zipOutputStream.write(binaryContent);
            zipOutputStream.closeEntry();
        }
        return buffer.toByteArray();
    }

    private static byte[] hexToBytes(String hex) {
        byte[] bytes = new byte[hex.length() / 2];
        for (int i = 0; i < bytes.length; i++) {
            int offset = i * 2;
            bytes[i] = (byte) Integer.parseInt(hex.substring(offset, offset + 2), 16);
        }
        return bytes;
    }

    private static byte[] zipOf(ZipEntryData... entries) throws Exception {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        try (ZipOutputStream zipOutputStream = new ZipOutputStream(buffer)) {
            for (ZipEntryData entry : entries) {
                zipOutputStream.putNextEntry(new ZipEntry(entry.path()));
                zipOutputStream.write(entry.content().getBytes(StandardCharsets.UTF_8));
                zipOutputStream.closeEntry();
            }
        }
        return buffer.toByteArray();
    }

    private record ZipEntryData(String path, String content) {
    }
}
