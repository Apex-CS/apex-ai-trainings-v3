package com.acme.pdfingestion.service;

import com.acme.pdfingestion.dto.StorageResponse;
import com.acme.pdfingestion.dto.StoreRequestResponse;
import com.acme.pdfingestion.model.DocumentChunk;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class PdfIngestionService {

    private final PdfService pdfService;
    private final ChunkingService chunkingService;
    private final ChromaStorageService chromaStorageService;
    private static final Logger log =
            LoggerFactory.getLogger(PdfIngestionService.class);

    public PdfIngestionService(PdfService pdfService, ChunkingService chunkingService, ChromaStorageService chromaStorageService) {
        this.pdfService = pdfService;
        this.chunkingService = chunkingService;
        this.chromaStorageService = chromaStorageService;
    }

    public StoreRequestResponse storePdf(String fileName)
            throws Exception {

        log.info("====================================================");
        log.info("RAG INGESTION PIPELINE");
        log.info("Source File: {}", fileName);
        log.info("====================================================");

        // ====================================================
        // STEP 1 - PDF DOCUMENT
        // ====================================================

        log.info("STEP 1 - PDF DOCUMENT");

        File pdfFile =
                new File("uploads/" + fileName);

        log.info("PDF Path: {}", pdfFile.getAbsolutePath());
        log.info("PDF Exists: {}", pdfFile.exists());

        // ====================================================
        // STEP 2 - TEXT EXTRACTION
        // ====================================================

        log.info("STEP 2 - TEXT EXTRACTION");

        String text =
                pdfService.extractText(pdfFile);

        log.info("Characters Extracted: {}", text.length());

        log.info("Text Preview:");
        log.info(
                "\n{}",
                text.substring(
                        0,
                        Math.min(500, text.length())
                )
        );

        // ====================================================
        // STEP 3 - CHUNKING (TEXT SPLITTING)
        // ====================================================

        log.info("STEP 3 - CHUNKING (TEXT SPLITTING)");

        List<DocumentChunk> chunks =
                chunkingService.chunkText(text);

        // ====================================================
        // STEP 4 - TEXT CHUNKS CREATED
        // ====================================================

        log.info("STEP 4 - TEXT CHUNKS CREATED");

        log.info("Total Chunks: {}", chunks.size());

        for (int i = 0; i < chunks.size(); i++) {

            DocumentChunk chunk = chunks.get(i);

            log.info("--------------------------------------------");
            log.info("Chunk #{}", i + 1);
            log.info("Chunk Id: {}", chunk.id());
            log.info("Chunk Length: {}", chunk.text().length());

            log.info(
                    "Chunk Preview:\n{}",
                    chunk.text().substring(
                            0,
                            Math.min(200, chunk.text().length())
                    )
            );
        }

        // ====================================================
        // STEP 5 - EMBEDDING MODEL
        // STEP 6 - CHUNK EMBEDDINGS
        // STEP 7 - VECTOR DATABASE (CHROMA)
        // ====================================================

        log.info("STEP 5 - EMBEDDING MODEL");
        log.info("Sending chunks to nomic-embed-text");

        log.info("STEP 6 - CHUNK EMBEDDINGS");
        log.info("Generating vector embeddings");

        log.info("STEP 7 - VECTOR DATABASE (CHROMA)");
        log.info("Storing chunks, embeddings and metadata");

        StorageResponse storageResponse =
                chromaStorageService.storeChunks(
                        fileName,
                        fileName,
                        chunks
                );

        log.info("--------------------------------------------");
        log.info("Collection: {}", storageResponse.collectionName());
        log.info("Chunks Stored: {}", storageResponse.chunksStored());
        log.info("Status: {}", storageResponse.status());

        StoreRequestResponse response =
                new StoreRequestResponse(
                        fileName,
                        storageResponse.collectionName(),
                        storageResponse.chunksStored(),
                        storageResponse.status()
                );

        log.info("====================================================");
        log.info("INGESTION COMPLETE");
        log.info("====================================================");

        return response;
    }
}