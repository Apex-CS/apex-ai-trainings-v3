package com.owasp.aiassistant.web;

import com.owasp.aiassistant.domain.IngestedDocument;
import com.owasp.aiassistant.dto.DocumentIngestRequest;
import com.owasp.aiassistant.dto.DocumentIngestResponse;
import com.owasp.aiassistant.rag.HtmlFileContentResolver;
import com.owasp.aiassistant.rag.RagDocumentService;
import com.owasp.aiassistant.rag.HtmlToMarkdownConverter;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

@RestController
@RequestMapping("/api/documents")
public class DocumentController {

    private final RagDocumentService ragDocumentService;
    private final HtmlToMarkdownConverter htmlToMarkdownConverter;
    private final HtmlFileContentResolver htmlFileContentResolver;

    public DocumentController(
            RagDocumentService ragDocumentService,
            HtmlToMarkdownConverter htmlToMarkdownConverter,
            HtmlFileContentResolver htmlFileContentResolver) {
        this.ragDocumentService = ragDocumentService;
        this.htmlToMarkdownConverter = htmlToMarkdownConverter;
        this.htmlFileContentResolver = htmlFileContentResolver;
    }

    @PostMapping("/ingest")
    public ResponseEntity<DocumentIngestResponse> ingestHtml(@Valid @RequestBody DocumentIngestRequest request) {
        IngestedDocument saved = ingestFromRequest(request);
        return ResponseEntity.ok(toResponse(saved));
    }

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<DocumentIngestResponse> uploadHtml(
            @RequestParam("documentId") String documentId,
            @RequestParam(value = "title", required = false) String title,
            @RequestParam("file") MultipartFile file) throws IOException {
        String html = new String(file.getBytes(), StandardCharsets.UTF_8);
        IngestedDocument saved = ragDocumentService.ingestHtml(
                documentId,
                title != null ? title : file.getOriginalFilename(),
                file.getOriginalFilename(),
                html);
        return ResponseEntity.ok(toResponse(saved));
    }

    @PostMapping("/{documentId}/replace")
    public ResponseEntity<DocumentIngestResponse> replaceDocument(
            @PathVariable String documentId,
            @Valid @RequestBody DocumentIngestRequest request) {
        if (!documentId.equals(request.documentId())) {
            throw new IllegalArgumentException("documentId in path must match request body");
        }
        IngestedDocument saved = ingestFromRequest(request);
        return ResponseEntity.ok(toResponse(saved));
    }

    @GetMapping
    public ResponseEntity<List<DocumentIngestResponse>> listDocuments() {
        List<DocumentIngestResponse> documents = ragDocumentService.listDocuments().stream()
                .map(this::toResponse)
                .toList();
        return ResponseEntity.ok(documents);
    }

    @PostMapping("/convert")
    public ResponseEntity<String> convertHtmlToMarkdown(@RequestBody String html) {
        return ResponseEntity.ok(htmlToMarkdownConverter.convert(html));
    }

    private IngestedDocument ingestFromRequest(DocumentIngestRequest request) {
        String html = htmlFileContentResolver.resolve(request);
        return ragDocumentService.ingestHtml(
                request.documentId(),
                request.title(),
                request.sourcePath(),
                html);
    }

    private DocumentIngestResponse toResponse(IngestedDocument document) {
        return new DocumentIngestResponse(
                document.getDocumentId(),
                document.getTitle(),
                document.getVersion(),
                document.getChunkCount(),
                document.getContentHash(),
                document.getUpdatedAt());
    }
}
