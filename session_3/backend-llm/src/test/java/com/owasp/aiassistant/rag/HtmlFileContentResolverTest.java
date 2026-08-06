package com.owasp.aiassistant.rag;

import com.owasp.aiassistant.dto.DocumentIngestRequest;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HtmlFileContentResolverTest {

    private final HtmlFileContentResolver resolver = new HtmlFileContentResolver();

    @Test
    void resolvesInlineHtmlWhenProvided() {
        String html = "<h1>Inline</h1>";

        String resolved = resolver.resolve(new DocumentIngestRequest(
                "doc-1", "Title", "/docs/inline.html", html, null));

        assertEquals(html, resolved);
    }

    @Test
    void inlineHtmlTakesPriorityOverFileLocation() {
        String html = "<h1>Inline wins</h1>";

        String resolved = resolver.resolve(new DocumentIngestRequest(
                "doc-1",
                "Title",
                "/docs/inline.html",
                html,
                "resources/html_files_rag/sample.html"));

        assertEquals(html, resolved);
    }

    @Test
    void resolvesHtmlFromClasspathLocation() {
        String resolved = resolver.resolve(new DocumentIngestRequest(
                "doc-1",
                "Title",
                "/docs/sample.html",
                null,
                "resources/html_files_rag/sample.html"));

        assertTrue(resolved.contains("Sample RAG Document"));
    }

    @Test
    void resolvesHtmlFromShortClasspathLocation() {
        String resolved = resolver.resolve(new DocumentIngestRequest(
                "doc-1", "Title", "/docs/sample.html", null, "sample.html"));

        assertTrue(resolved.contains("Sample RAG Document"));
    }

    @Test
    void rejectsMissingFile() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                resolver.resolve(new DocumentIngestRequest(
                        "doc-1", "Title", "/docs/missing.html", null, "missing.html")));

        assertTrue(ex.getMessage().contains("HTML file not found"));
    }

    @Test
    void rejectsPathTraversal() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                resolver.resolve(new DocumentIngestRequest(
                        "doc-1", "Title", "/docs/evil.html", null, "../application.yml")));

        assertTrue(ex.getMessage().contains(".."));
    }

    @Test
    void rejectsWhenNeitherSourceProvided() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                resolver.resolve(new DocumentIngestRequest("doc-1", "Title", "/docs/empty.html", null, null)));

        assertTrue(ex.getMessage().contains("Either html or html_file_location must be provided"));
    }

    @Test
    void normalizesKnownPrefixes() {
        assertEquals("the_file.html", HtmlFileContentResolver.normalizeLocation("resources/html_files_rag/the_file.html"));
        assertEquals("the_file.html", HtmlFileContentResolver.normalizeLocation("html_files_rag/the_file.html"));
        assertEquals("the_file.html", HtmlFileContentResolver.normalizeLocation("src/main/resources/html_files_rag/the_file.html"));
    }
}
