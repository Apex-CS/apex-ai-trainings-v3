package com.owasp.aiassistant.codereview;

import com.owasp.aiassistant.config.CodeReviewProperties;
import com.owasp.aiassistant.dto.CodeAttachment;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@Service
public class CodeReviewPayloadProcessor {

    private static final Set<String> ALLOWED_EXTENSIONS = Set.of(
            ".py", ".java", ".js", ".ts", ".tsx", ".jsx", ".html", ".htm", ".css", ".xml",
            ".yml", ".yaml", ".properties", ".md", ".sql", ".json", ".gradle", ".kt", ".go",
            ".rb", ".php", ".cs", ".vue", ".scss", ".less", ".sh", ".bat", ".c", ".cpp",
            ".h", ".hpp", ".env", ".rs", ".swift", ".toml", ".ini", ".gitignore", ".dockerignore");

    private static final Set<String> ALLOWED_FILENAMES = Set.of(
            "dockerfile", "makefile", "readme", "license", "procfile");

    private static final List<String> DENIED_PATH_SEGMENTS = List.of(
            "node_modules/", ".git/", "target/", "build/", "dist/", "__pycache__/",
            ".venv/", "venv/", ".idea/", ".gradle/", ".mvn/", "vendor/", ".next/",
            "coverage/", ".pytest_cache/", ".tox/");

    private final CodeReviewProperties properties;

    public CodeReviewPayloadProcessor(CodeReviewProperties properties) {
        this.properties = properties;
    }

    public String prepareForReview(CodeAttachment attachment, String focus) {
        validateAttachment(attachment);

        byte[] bytes = decodeBase64(attachment.data());
        if (bytes.length > properties.getMaxUploadBytes()) {
            throw new IllegalArgumentException(
                    "Attachment exceeds maximum upload size of " + properties.getMaxUploadBytes() + " bytes");
        }

        String extension = extension(attachment.filename());
        if (isZip(extension, attachment.contentType())) {
            return formatReviewBundle(attachment.filename(), focus, processZip(bytes));
        }

        if (!isAllowedTextFile(attachment.filename())) {
            throw new IllegalArgumentException(
                    "Unsupported file type for code review: " + attachment.filename());
        }

        String content = decodeText(bytes);
        return formatReviewBundle(
                attachment.filename(),
                focus,
                new ReviewBundle(List.of(attachment.filename()), Map.of(attachment.filename(), content), List.of()));
    }

    private void validateAttachment(CodeAttachment attachment) {
        if (!"base64".equalsIgnoreCase(attachment.encoding())) {
            throw new IllegalArgumentException("Only base64 encoding is supported");
        }
        if (attachment.filename() == null || attachment.filename().isBlank()) {
            throw new IllegalArgumentException("Attachment filename is required");
        }
    }

    private static byte[] decodeBase64(String data) {
        try {
            return Base64.getDecoder().decode(data);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Attachment data is not valid base64", e);
        }
    }

    private ReviewBundle processZip(byte[] zipBytes) {
        Map<String, String> files = new LinkedHashMap<>();
        List<String> skipped = new ArrayList<>();
        long uncompressedTotal = 0;

        try (ZipInputStream zipInputStream = new ZipInputStream(new ByteArrayInputStream(zipBytes))) {
            ZipEntry entry;
            while ((entry = zipInputStream.getNextEntry()) != null) {
                if (entry.isDirectory()) {
                    continue;
                }

                String normalizedPath = normalizeZipPath(entry.getName());
                if (isDeniedPath(normalizedPath)) {
                    skipped.add(normalizedPath + " (excluded path)");
                    continue;
                }

                if (!isAllowedTextFile(normalizedPath)) {
                    skipped.add(normalizedPath + " (unsupported type)");
                    continue;
                }

                if (files.size() >= properties.getMaxFiles()) {
                    skipped.add(normalizedPath + " (file limit reached)");
                    continue;
                }

                byte[] entryBytes = readEntryBytes(zipInputStream, entry);
                uncompressedTotal += entryBytes.length;
                if (uncompressedTotal > properties.getMaxUncompressedBytes()) {
                    throw new IllegalArgumentException(
                            "Uncompressed archive exceeds maximum size of "
                                    + properties.getMaxUncompressedBytes() + " bytes");
                }

                if (isBinaryContent(entryBytes)) {
                    skipped.add(normalizedPath + " (binary content)");
                    continue;
                }

                files.put(normalizedPath, new String(entryBytes, StandardCharsets.UTF_8));
            }
        } catch (IOException e) {
            throw new IllegalArgumentException("Failed to read zip archive: " + e.getMessage(), e);
        }

        if (files.isEmpty()) {
            throw new IllegalArgumentException(
                    "Zip archive contains no reviewable text files. Supported types include .py, .java, .html, and similar source files.");
        }

        List<String> paths = new ArrayList<>(files.keySet());
        paths.sort(Comparator.naturalOrder());
        return new ReviewBundle(paths, files, skipped);
    }

    private static byte[] readEntryBytes(ZipInputStream zipInputStream, ZipEntry entry) throws IOException {
        long declaredSize = entry.getSize();
        if (declaredSize > 0 && declaredSize > 2L * 1024 * 1024) {
            throw new IllegalArgumentException("Zip entry is too large: " + entry.getName());
        }

        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        byte[] chunk = new byte[8192];
        long totalRead = 0;
        int read;
        while ((read = zipInputStream.read(chunk)) >= 0) {
            totalRead += read;
            if (totalRead > 2L * 1024 * 1024) {
                throw new IllegalArgumentException("Zip entry is too large: " + entry.getName());
            }
            buffer.write(chunk, 0, read);
        }
        return buffer.toByteArray();
    }

    private String formatReviewBundle(String sourceName, String focus, ReviewBundle bundle) {
        String normalizedFocus = focus == null || focus.isBlank() ? "all" : focus.trim();
        StringBuilder output = new StringBuilder();
        output.append("Source: ").append(sourceName).append('\n');
        output.append("Review focus: ").append(normalizedFocus).append("\n\n");
        output.append("=== PROJECT STRUCTURE ===\n");
        output.append(buildDirectoryTree(bundle.paths())).append('\n');

        if (!bundle.skipped().isEmpty()) {
            output.append("\n=== SKIPPED FILES ===\n");
            for (String skipped : bundle.skipped()) {
                output.append("- ").append(skipped).append('\n');
            }
        }

        output.append("\n=== FILE CONTENTS ===\n");
        int remainingChars = properties.getMaxTextChars();
        boolean truncated = false;

        for (String path : bundle.paths()) {
            String sectionHeader = "\n--- " + path + " ---\n";
            String content = bundle.files().get(path);
            int sectionLength = sectionHeader.length() + content.length();

            if (sectionLength > remainingChars) {
                if (remainingChars <= sectionHeader.length() + 20) {
                    truncated = true;
                    break;
                }
                int allowedContent = remainingChars - sectionHeader.length();
                output.append(sectionHeader);
                output.append(content, 0, Math.min(content.length(), allowedContent));
                output.append("\n... [truncated due to size limit] ...\n");
                truncated = true;
                break;
            }

            output.append(sectionHeader).append(content).append('\n');
            remainingChars -= sectionLength;
        }

        if (truncated) {
            output.append("\nNote: Additional files were omitted because the combined content exceeded the review size limit.\n");
        }

        return output.toString();
    }

    static String buildDirectoryTree(List<String> paths) {
        TreeNode root = new TreeNode();
        for (String path : paths) {
            String[] parts = path.split("/");
            TreeNode current = root;
            for (int i = 0; i < parts.length; i++) {
                String part = parts[i];
                boolean isFile = i == parts.length - 1;
                current = current.child(part, isFile);
            }
        }
        StringBuilder tree = new StringBuilder();
        appendTree(root, "", tree, true);
        return tree.toString().stripTrailing();
    }

    private static void appendTree(TreeNode node, String prefix, StringBuilder tree, boolean isRoot) {
        List<Map.Entry<String, TreeNode>> children = node.sortedChildren();
        for (int i = 0; i < children.size(); i++) {
            Map.Entry<String, TreeNode> child = children.get(i);
            boolean last = i == children.size() - 1;
            if (!isRoot) {
                tree.append(prefix).append(last ? "└── " : "├── ").append(child.getKey()).append('\n');
            } else {
                tree.append(child.getKey()).append('\n');
            }
            String childPrefix = isRoot ? "" : prefix + (last ? "    " : "│   ");
            appendTree(child.getValue(), childPrefix, tree, false);
        }
    }

    private static String decodeText(byte[] bytes) {
        if (isBinaryContent(bytes)) {
            throw new IllegalArgumentException("File contains binary content and cannot be reviewed as text");
        }
        return new String(bytes, StandardCharsets.UTF_8);
    }

    private static boolean isBinaryContent(byte[] bytes) {
        for (byte b : bytes) {
            if (b == 0) {
                return true;
            }
        }
        return new String(bytes, StandardCharsets.UTF_8).indexOf('\uFFFD') >= 0;
    }

    private static boolean isZip(String extension, String contentType) {
        return ".zip".equals(extension)
                || (contentType != null && contentType.toLowerCase(Locale.ROOT).contains("zip"));
    }

    private static String extension(String filename) {
        int dot = filename.lastIndexOf('.');
        if (dot < 0 || dot == filename.length() - 1) {
            return "";
        }
        return filename.substring(dot).toLowerCase(Locale.ROOT);
    }

    private static String baseName(String filename) {
        int slash = Math.max(filename.lastIndexOf('/'), filename.lastIndexOf('\\'));
        String name = slash >= 0 ? filename.substring(slash + 1) : filename;
        return name.toLowerCase(Locale.ROOT);
    }

    private static boolean isAllowedTextFile(String path) {
        String fileName = baseName(path);
        if (ALLOWED_FILENAMES.contains(fileName)) {
            return true;
        }
        String ext = extension(fileName);
        return ALLOWED_EXTENSIONS.contains(ext);
    }

    private static boolean isDeniedPath(String path) {
        String normalized = path.replace('\\', '/').toLowerCase(Locale.ROOT);
        if (normalized.startsWith("/") || normalized.contains("..")) {
            return true;
        }
        for (String denied : DENIED_PATH_SEGMENTS) {
            if (normalized.contains(denied)) {
                return true;
            }
        }
        return false;
    }

    private static String normalizeZipPath(String entryName) {
        String normalized = entryName.replace('\\', '/');
        while (normalized.startsWith("./")) {
            normalized = normalized.substring(2);
        }
        if (normalized.startsWith("/")) {
            throw new IllegalArgumentException("Unsafe zip entry path: " + entryName);
        }
        if (normalized.contains("..")) {
            throw new IllegalArgumentException("Unsafe zip entry path: " + entryName);
        }
        return normalized;
    }

    private record ReviewBundle(List<String> paths, Map<String, String> files, List<String> skipped) {
    }

    private static final class TreeNode {
        private final Map<String, TreeNode> children = new LinkedHashMap<>();

        private TreeNode child(String name, boolean file) {
            return children.computeIfAbsent(name, key -> new TreeNode());
        }

        private List<Map.Entry<String, TreeNode>> sortedChildren() {
            return children.entrySet().stream()
                    .sorted(Comparator.comparing(Map.Entry::getKey))
                    .toList();
        }
    }
}
