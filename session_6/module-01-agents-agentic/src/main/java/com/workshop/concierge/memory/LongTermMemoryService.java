package com.workshop.concierge.memory;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingSearchRequest;
import dev.langchain4j.store.embedding.EmbeddingSearchResult;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.filter.Filter;
import dev.langchain4j.store.embedding.filter.MetadataFilterBuilder;

/**
 * Long-Term Memory — persists per-user preferences and allergies as embeddings in a vector
 * store so they can be semantically retrieved on every subsequent request, keyed by userId.
 */
@Service
public class LongTermMemoryService {

    private static final Logger log = LoggerFactory.getLogger(LongTermMemoryService.class);
    private static final String USER_ID_KEY = "userId";
    private static final String RETRIEVAL_QUERY = "user dietary preferences, allergies and goals";

    private final EmbeddingStore<TextSegment> embeddingStore;
    private final EmbeddingModel embeddingModel;

    public LongTermMemoryService(EmbeddingStore<TextSegment> embeddingStore, EmbeddingModel embeddingModel) {
        this.embeddingStore = embeddingStore;
        this.embeddingModel = embeddingModel;
    }

    public void savePreference(String userId, String preferenceText) {
        Metadata metadata = Metadata.from(USER_ID_KEY, userId);
        TextSegment segment = TextSegment.from(preferenceText, metadata);
        Embedding embedding = embeddingModel.embed(segment).content();
        embeddingStore.add(embedding, segment);
        log.info("[LongTermMemory] stored preference for userId={}: {}", userId, preferenceText);
    }

    public List<String> getPreferences(String userId) {
        Embedding queryEmbedding = embeddingModel.embed(RETRIEVAL_QUERY).content();
        Filter userFilter = MetadataFilterBuilder.metadataKey(USER_ID_KEY).isEqualTo(userId);

        EmbeddingSearchRequest request = EmbeddingSearchRequest.builder()
                .queryEmbedding(queryEmbedding)
                .maxResults(50)
                .minScore(0.0)
                .filter(userFilter)
                .build();

        EmbeddingSearchResult<TextSegment> result = embeddingStore.search(request);
        return result.matches().stream()
                .map(match -> match.embedded().text())
                .toList();
    }
}
