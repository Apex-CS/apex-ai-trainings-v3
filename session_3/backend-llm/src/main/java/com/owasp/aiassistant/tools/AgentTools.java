package com.owasp.aiassistant.tools;

import com.owasp.aiassistant.agent.CodeReviewContext;
import com.owasp.aiassistant.codereview.CodeReviewPayloadProcessor;
import com.owasp.aiassistant.dto.CodeAttachment;
import com.owasp.aiassistant.exception.ToolConnectivityException;
import com.owasp.aiassistant.rag.RagDocumentService;
import com.owasp.aiassistant.search.WebSearchService;
import com.owasp.aiassistant.sql.SqlQueryService;
import org.springframework.ai.document.Document;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class AgentTools {

    private static final String KNOWLEDGE_BASE_TOOL = "searchKnowledgeBase";

    private final RagDocumentService ragDocumentService;
    private final WebSearchService webSearchService;
    private final SqlQueryService sqlQueryService;
    private final CodeReviewContext codeReviewContext;
    private final CodeReviewPayloadProcessor codeReviewPayloadProcessor;

    public AgentTools(
            RagDocumentService ragDocumentService,
            WebSearchService webSearchService,
            SqlQueryService sqlQueryService,
            CodeReviewContext codeReviewContext,
            CodeReviewPayloadProcessor codeReviewPayloadProcessor) {
        this.ragDocumentService = ragDocumentService;
        this.webSearchService = webSearchService;
        this.sqlQueryService = sqlQueryService;
        this.codeReviewContext = codeReviewContext;
        this.codeReviewPayloadProcessor = codeReviewPayloadProcessor;
    }

    @Tool(description = """
            Search the internal knowledge base (RAG vector store) for information from ingested HTML/Markdown documents.
            Use this when the user asks about content that may have been uploaded or ingested into the system.
            """)
    public String searchKnowledgeBase(
            @ToolParam(description = "Natural language search query") String query) {
        if (!ragDocumentService.isEnabled()) {
            return "The knowledge base (RAG) is not enabled. Use web search or SQL tools instead.";
        }
        try {
            List<Document> results = ragDocumentService.search(query);
            if (results.isEmpty()) {
                return "No matching documents found in the knowledge base.";
            }
            return results.stream()
                    .map(this::formatDocument)
                    .collect(Collectors.joining("\n\n---\n\n"));
        } catch (Exception e) {
            if (ToolErrorClassifier.isConnectivityError(e)) {
                throw new ToolConnectivityException(KNOWLEDGE_BASE_TOOL, "Knowledge base search failed: " + e.getMessage(), e);
            }
            return "Knowledge base search failed: " + e.getMessage();
        }
    }

    @Tool(description = """
            Search the public web for current information, news, or facts not available in the internal knowledge base.
            Use this for recent events or general knowledge outside ingested documents.
            """)
    public String searchWeb(
            @ToolParam(description = "Web search query") String query) {
        return webSearchService.search(query);
    }

    @Tool(description = """
            Execute a read-only SQL SELECT query against the PostgreSQL database.
            Use this for structured Example Company data (finance, IT, marketing, sales) or ingested document metadata.
            Only SELECT queries are allowed.
            """)
    public String queryDatabase(
            @ToolParam(description = "Read-only SQL SELECT query") String sql) {
        return sqlQueryService.executeReadOnlyQuery(sql);
    }

    @Tool(description = """
            Describe available SQL tables and columns in the PostgreSQL database.
            Call this before writing SQL if you are unsure of the schema.
            """)
    public String describeDatabaseSchema() {
        return sqlQueryService.describeSchema();
    }

    @Tool(description = """
            Review code attached by the user for security issues (OWASP Top 10), bugs, and best practices.
            Use this when the user asks for a code review or has attached a .py, .html, or .zip file.
            The attached file is decoded from graph state; for zip archives the tool expands and filters source files.
            """)
    public String reviewCode(
            @ToolParam(description = "Optional focus area such as 'SQL injection', 'XSS', 'auth', or 'all'")
            String focus) {
        CodeAttachment attachment = codeReviewContext.get()
                .orElse(null);
        if (attachment == null) {
            return "No code attachment is available for this conversation turn. Ask the user to attach a .py, .html, or .zip file.";
        }

        try {
            String prepared = codeReviewPayloadProcessor.prepareForReview(
                    attachment,
                    focus == null || focus.isBlank() ? "all" : focus);
            return prepared;
        } catch (IllegalArgumentException e) {
            return "Could not prepare code for review: " + e.getMessage();
        }
    }

    private String formatDocument(Document document) {
        String title = String.valueOf(document.getMetadata().getOrDefault("title", "Untitled"));
        String source = String.valueOf(document.getMetadata().getOrDefault("source_path", ""));
        return "Title: " + title + "\nSource: " + source + "\n\n" + document.getText();
    }
}
