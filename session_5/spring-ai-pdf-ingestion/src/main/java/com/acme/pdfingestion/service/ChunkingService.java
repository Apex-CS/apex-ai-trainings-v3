package com.acme.pdfingestion.service;

import com.acme.pdfingestion.model.DocumentChunk;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class ChunkingService {

    private static final int CHUNK_SIZE = 500;
    private static final int CHUNK_OVERLAP = 100;

    public List<DocumentChunk> chunkText(String text) {

        List<DocumentChunk> chunks = new ArrayList<>();

        int start = 0;
        int chunkNumber = 1;

        while (start < text.length()) {

            int end =
                    Math.min(
                            start + CHUNK_SIZE,
                            text.length());

            String chunkText =
                    text.substring(start, end);

            chunks.add(
                    new DocumentChunk(
                            UUID.randomUUID().toString(),
                            chunkNumber++,
                            chunkText
                    )
            );

            if (end == text.length()) {
                break;
            }

            start += (CHUNK_SIZE - CHUNK_OVERLAP);
        }

        return chunks;
    }
}
