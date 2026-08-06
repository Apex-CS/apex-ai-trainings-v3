package com.owasp.aiassistant.rag;

import com.owasp.aiassistant.dto.DocumentIngestRequest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

@Component
public class HtmlFileContentResolver {

    private static final String RAG_HTML_DIR = "html_files_rag/";

    public String resolve(DocumentIngestRequest request) {
        if (isNotBlank(request.html())) {
            return request.html();
        }

        String location = request.htmlFileLocation();
        if (!isNotBlank(location)) {
            throw new IllegalArgumentException("Either html or html_file_location must be provided");
        }

        String relativePath = normalizeLocation(location);
        Resource resource = new ClassPathResource(RAG_HTML_DIR + relativePath);
        if (!resource.exists()) {
            throw new IllegalArgumentException("HTML file not found: " + location);
        }

        try (InputStream inputStream = resource.getInputStream()) {
            return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException ex) {
            throw new IllegalArgumentException("Failed to read HTML file: " + location, ex);
        }
    }

    static String normalizeLocation(String location) {
        String normalized = location.trim().replace('\\', '/');
        while (normalized.startsWith("/")) {
            normalized = normalized.substring(1);
        }

        String[] prefixes = {
                "src/main/resources/html_files_rag/",
                "resources/html_files_rag/",
                "classpath:html_files_rag/",
                "html_files_rag/"
        };
        for (String prefix : prefixes) {
            if (normalized.startsWith(prefix)) {
                normalized = normalized.substring(prefix.length());
                break;
            }
        }

        if (normalized.contains("..")) {
            throw new IllegalArgumentException("html_file_location must not contain '..'");
        }
        if (normalized.isBlank()) {
            throw new IllegalArgumentException("html_file_location must not be blank");
        }

        return normalized;
    }

    private static boolean isNotBlank(String value) {
        return value != null && !value.isBlank();
    }
}
