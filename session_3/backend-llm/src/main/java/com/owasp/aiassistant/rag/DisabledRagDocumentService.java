package com.owasp.aiassistant.rag;

import com.owasp.aiassistant.domain.IngestedDocument;
import org.springframework.ai.document.Document;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@ConditionalOnProperty(name = "app.rag.enabled", havingValue = "false", matchIfMissing = true)
public class DisabledRagDocumentService implements RagDocumentService {

    private static final String MESSAGE =
            "RAG is disabled. Set app.rag.enabled=true and configure EMBEDDING_ENDPOINT_NAME to ingest and search documents.";

    @Override
    public IngestedDocument ingestHtml(String documentId, String title, String sourcePath, String html) {
        throw new IllegalStateException(MESSAGE);
    }

    @Override
    public IngestedDocument ingestMarkdown(String documentId, String title, String sourcePath, String markdown) {
        throw new IllegalStateException(MESSAGE);
    }

    @Override
    public List<Document> search(String query) {
        return List.of();
    }

    @Override
    public List<IngestedDocument> listDocuments() {
        return List.of();
    }

    @Override
    public boolean isEnabled() {
        return false;
    }
}
