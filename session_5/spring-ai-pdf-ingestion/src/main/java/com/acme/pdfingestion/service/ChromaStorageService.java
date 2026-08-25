package com.acme.pdfingestion.service;

import com.acme.pdfingestion.dto.StorageResponse;
import com.acme.pdfingestion.model.DocumentChunk;
import org.springframework.ai.chroma.vectorstore.ChromaApi;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class ChromaStorageService {

    private static final Logger log = LoggerFactory.getLogger(ChromaStorageService.class);

    private static final String TENANT = "default_tenant";
    private static final String DATABASE = "default_database";
    private static final String COLLECTION_NAME = "pdf-demo";

    private final ChromaApi chromaApi;
    private final EmbeddingService embeddingService;

    public ChromaStorageService(
            ChromaApi chromaApi,
            EmbeddingService embeddingService) {

        this.chromaApi = chromaApi;
        this.embeddingService = embeddingService;
    }

    public StorageResponse storeChunks(
            String documentId,
            String sourceFile,
            List<DocumentChunk> chunks) {

        log.info("====================================================");
        log.info("RAG INGESTION - STORAGE PHASE");
        log.info("====================================================");

        ChromaApi.Collection collection;

        try {

            log.info("Locating Chroma collection: {}", COLLECTION_NAME);

            collection = chromaApi.getCollection(
                    TENANT,
                    DATABASE,
                    COLLECTION_NAME
            );

            log.info("Collection found.");
            log.info("Collection Id: {}", collection.id());

        } catch (Exception ex) {

            log.info("Collection not found.");
            log.info("Creating collection: {}", COLLECTION_NAME);

            collection = chromaApi.createCollection(
                    TENANT,
                    DATABASE,
                    new ChromaApi.CreateCollectionRequest(
                            COLLECTION_NAME
                    )
            );

            log.info("Collection created.");
            log.info("Collection Id: {}", collection.id());
        }

        String collectionId = collection.id();

        // ====================================================
        // STEP 5 - EMBEDDING MODEL
        // ====================================================

        log.info("====================================================");
        log.info("STEP 5 - EMBEDDING MODEL");
        log.info("Generating embeddings using nomic-embed-text");
        log.info("Chunks To Process: {}", chunks.size());
        log.info("====================================================");

        List<float[]> embeddings = new ArrayList<>();

        for (DocumentChunk chunk : chunks) {

            float[] embedding =
                    embeddingService.generateRawEmbedding(chunk);

            embeddings.add(embedding);

            log.info(
                    "Embedding generated for Chunk {} (Dimensions: {})",
                    chunk.chunkNumber(),
                    embedding.length
            );
        }

        // ====================================================
        // STEP 6 - CHUNK EMBEDDINGS
        // ====================================================

        log.info("====================================================");
        log.info("STEP 6 - CHUNK EMBEDDINGS");
        log.info("Combining chunks, vectors and metadata");
        log.info("====================================================");

        List<ChromaApi.AddEmbeddingsRequest> requests =
                new ArrayList<>();

        for (int i = 0; i < chunks.size(); i++) {

            DocumentChunk chunk = chunks.get(i);

            float[] embedding = embeddings.get(i);

            ChromaApi.AddEmbeddingsRequest request =
                    new ChromaApi.AddEmbeddingsRequest(
                            chunk.id(),
                            embedding,
                            Map.of(
                                    "documentId", documentId,
                                    "sourceFile", sourceFile,
                                    "chunkNumber", chunk.chunkNumber()
                            ),
                            chunk.text()
                    );

            requests.add(request);

            log.info("--------------------------------------------");
            log.info("Chunk {}", chunk.chunkNumber());
            log.info("Chunk Id: {}", chunk.id());
            log.info("Embedding Dimensions: {}", embedding.length);
            log.info("Metadata:");
            log.info("  documentId : {}", documentId);
            log.info("  sourceFile : {}", sourceFile);
            log.info("  chunkNumber: {}", chunk.chunkNumber());
        }

        log.info("Total Chunk Embeddings Created: {}", requests.size());

        // ====================================================
        // STEP 7 - VECTOR DATABASE (CHROMA)
        // ====================================================

        log.info("====================================================");
        log.info("STEP 7 - VECTOR DATABASE (CHROMA)");
        log.info("Storing chunks, embeddings and metadata");
        log.info("Collection: {}", COLLECTION_NAME);
        log.info("Collection Id: {}", collectionId);
        log.info("====================================================");

        for (ChromaApi.AddEmbeddingsRequest request : requests) {

            chromaApi.upsertEmbeddings(
                    TENANT,
                    DATABASE,
                    collectionId,
                    request
            );
        }

        log.info("Storage completed successfully.");
        log.info("Chunks Stored: {}", requests.size());

        log.info("====================================================");
        log.info("RAG STORAGE PHASE COMPLETE");
        log.info("====================================================");

        return new StorageResponse(
                COLLECTION_NAME,
                chunks.size(),
                chunks.size(),
                "SUCCESS"
        );
    }
}