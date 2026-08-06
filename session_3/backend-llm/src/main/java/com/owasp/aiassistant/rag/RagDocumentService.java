package com.owasp.aiassistant.rag;

import com.owasp.aiassistant.domain.IngestedDocument;
import org.springframework.ai.document.Document;

import java.util.List;

public interface RagDocumentService {

    IngestedDocument ingestHtml(String documentId, String title, String sourcePath, String html);

    IngestedDocument ingestMarkdown(String documentId, String title, String sourcePath, String markdown);

    List<Document> search(String query);

    List<IngestedDocument> listDocuments();

    boolean isEnabled();
}
