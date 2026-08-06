package com.owasp.aiassistant.rag;

import com.owasp.aiassistant.domain.IngestedDocument;
import com.owasp.aiassistant.repository.IngestedDocumentRepository;
import org.springframework.ai.document.Document;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.Filter;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@ConditionalOnProperty(name = "app.rag.enabled", havingValue = "true")
public class DocumentIngestionService implements RagDocumentService {

    private final HtmlToMarkdownConverter htmlToMarkdownConverter;
    private final VectorStore vectorStore;
    private final IngestedDocumentRepository documentRepository;
    private final int chunkSize;
    private final int chunkOverlap;
    private final int topK;

    public DocumentIngestionService(
            HtmlToMarkdownConverter htmlToMarkdownConverter,
            VectorStore vectorStore,
            IngestedDocumentRepository documentRepository,
            @Value("${app.rag.chunk-size}") int chunkSize,
            @Value("${app.rag.chunk-overlap}") int chunkOverlap,
            @Value("${app.rag.top-k}") int topK) {
        this.htmlToMarkdownConverter = htmlToMarkdownConverter;
        this.vectorStore = vectorStore;
        this.documentRepository = documentRepository;
        this.chunkSize = chunkSize;
        this.chunkOverlap = chunkOverlap;
        this.topK = topK;
    }

    @Transactional
    public IngestedDocument ingestHtml(String documentId, String title, String sourcePath, String html) {
        String markdown = htmlToMarkdownConverter.convert(html);
        return ingestMarkdown(documentId, title, sourcePath, markdown);
    }

    @Transactional
    public IngestedDocument ingestMarkdown(String documentId, String title, String sourcePath, String markdown) {
        if (documentId == null || documentId.isBlank()) {
            throw new IllegalArgumentException("documentId is required");
        }
        if (markdown == null || markdown.isBlank()) {
            throw new IllegalArgumentException("markdown content must not be blank");
        }

        removeFromVectorStore(documentId);

        List<Document> chunks = splitIntoChunks(documentId, title, sourcePath, markdown);
        vectorStore.add(chunks);

        String contentHash = sha256(markdown);
        IngestedDocument record = documentRepository.findByDocumentId(documentId)
                .orElseGet(IngestedDocument::new);

        record.setDocumentId(documentId);
        record.setTitle(title);
        record.setSourcePath(sourcePath);
        record.setVersion(record.getId() == null ? 1 : record.getVersion() + 1);
        record.setChunkCount(chunks.size());
        record.setContentHash(contentHash);
        if (record.getIngestedAt() == null) {
            record.setIngestedAt(Instant.now());
        }
        record.setUpdatedAt(Instant.now());

        return documentRepository.save(record);
    }

    public List<Document> search(String query) {
        return vectorStore.similaritySearch(
                SearchRequest.builder()
                        .query(query)
                        .topK(topK)
                        .build());
    }

    public List<IngestedDocument> listDocuments() {
        return documentRepository.findAll();
    }

    @Override
    public boolean isEnabled() {
        return true;
    }

    private void removeFromVectorStore(String documentId) {
        Filter.Expression filter = new FilterExpressionBuilder()
                .eq("document_id", documentId)
                .build();
        vectorStore.delete(filter);
    }

    private List<Document> splitIntoChunks(String documentId, String title, String sourcePath, String markdown) {
        Document source = new Document(markdown, Map.of(
                "document_id", documentId,
                "title", title != null ? title : documentId,
                "source_path", sourcePath != null ? sourcePath : "",
                "format", "markdown"
        ));

        TokenTextSplitter splitter = new TokenTextSplitter(chunkSize, chunkOverlap, 5, 10000, true);
        return splitter.apply(List.of(source)).stream()
                .map(chunk -> {
                    Map<String, Object> metadata = chunk.getMetadata();
                    metadata.put("document_id", documentId);
                    metadata.put("title", title != null ? title : documentId);
                    if (sourcePath != null) {
                        metadata.put("source_path", sourcePath);
                    }
                    return new Document(chunk.getText(), metadata);
                })
                .collect(Collectors.toList());
    }

    private static String sha256(String content) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(content.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}
