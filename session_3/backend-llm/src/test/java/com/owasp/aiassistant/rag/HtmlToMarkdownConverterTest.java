package com.owasp.aiassistant.rag;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class HtmlToMarkdownConverterTest {

    private final HtmlToMarkdownConverter converter = new HtmlToMarkdownConverter();

    @Test
    void convertsBasicHtmlToMarkdown() {
        String markdown = converter.convert("<h1>OWASP</h1><p>Security <strong>guide</strong>.</p>");
        assertTrue(markdown.contains("OWASP"));
        assertTrue(markdown.contains("Security"));
    }
}
