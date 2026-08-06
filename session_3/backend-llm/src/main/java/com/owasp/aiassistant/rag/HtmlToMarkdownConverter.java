package com.owasp.aiassistant.rag;

import com.vladsch.flexmark.html2md.converter.FlexmarkHtmlConverter;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Converts HTML content to Markdown for RAG ingestion.
 */
@Component
public class HtmlToMarkdownConverter {

    private final FlexmarkHtmlConverter converter = FlexmarkHtmlConverter.builder().build();

    public String convert(String html) {
        if (html == null || html.isBlank()) {
            throw new IllegalArgumentException("HTML content must not be blank");
        }
        return converter.convert(html).trim();
    }

    public String convertFile(Path htmlFile) throws IOException {
        String html = Files.readString(htmlFile, StandardCharsets.UTF_8);
        return convert(html);
    }
}
