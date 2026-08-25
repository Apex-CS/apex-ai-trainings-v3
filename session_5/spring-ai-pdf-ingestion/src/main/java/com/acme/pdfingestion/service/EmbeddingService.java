package com.acme.pdfingestion.service;

import com.acme.pdfingestion.dto.EmbeddingResponse;
import com.acme.pdfingestion.model.DocumentChunk;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class EmbeddingService {

    private final EmbeddingModel embeddingModel;

    public EmbeddingService(EmbeddingModel embeddingModel) {
        this.embeddingModel = embeddingModel;
    }

    public EmbeddingResponse generateEmbedding(
            String documentId,
            DocumentChunk chunk) {

        float[] embedding =
                embeddingModel.embed(chunk.text());

        List<Float> preview = new ArrayList<>();

        for (int i = 0; i < Math.min(25, embedding.length); i++) {
            preview.add(embedding[i]);
        }

        return new EmbeddingResponse(
                documentId,
                chunk.chunkNumber(),
                embedding.length,
                preview
        );
    }

    public float[] generateRawEmbedding(
            DocumentChunk chunk) {

        return embeddingModel.embed(chunk.text());
    }
}