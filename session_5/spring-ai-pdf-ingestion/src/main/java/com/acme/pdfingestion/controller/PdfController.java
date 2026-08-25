package com.acme.pdfingestion.controller;

import com.acme.pdfingestion.dto.*;
import com.acme.pdfingestion.model.DocumentChunk;
import com.acme.pdfingestion.service.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.util.List;

@RestController
@RequestMapping("/api/pdf")
@Tag(name = "PDF Ingestion")
public class PdfController {

    private final PdfStorageService pdfStorageService;
    private final PdfService pdfService;
    private final ChunkingService chunkingService;
    private final EmbeddingService embeddingService;
    private final PdfIngestionService pdfIngestionService;

    public PdfController(
            PdfStorageService pdfStorageService,
            PdfService pdfService,
            ChunkingService chunkingService,
            EmbeddingService embeddingService,
            ChromaStorageService chromaStorageService,
            PdfIngestionService pdfIngestionService) {

        this.pdfStorageService = pdfStorageService;
        this.pdfService = pdfService;
        this.chunkingService = chunkingService;
        this.embeddingService = embeddingService;
        this.pdfIngestionService = pdfIngestionService;
    }

    @PostMapping(
            value = "/upload",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    @Operation(summary = "Upload a PDF document")
    public UploadedDocumentResponse upload(
            @RequestParam("file")
            MultipartFile file) throws Exception {

        String storedFileName =
                pdfStorageService.store(file);

        return new UploadedDocumentResponse(
                storedFileName,
                file.getOriginalFilename(),
                file.getSize(),
                "UPLOADED"
        );
    }

    @GetMapping("/text/{fileName}")
    @Operation(summary = "Extract text from a stored PDF")
    public ExtractedTextResponse extractText(
            @PathVariable String fileName)
            throws Exception {

        File pdfFile =
                new File("uploads/" + fileName);

        String text =
                pdfService.extractText(pdfFile);

        return new ExtractedTextResponse(
                fileName,
                text.length(),
                text
        );
    }

    @GetMapping("/chunks/{fileName}")
    @Operation(summary = "Generate chunks from a PDF")
    public ChunkingResponse generateChunks(
            @PathVariable String fileName)
            throws Exception {

        File pdfFile =
                new File("uploads/" + fileName);

        String text =
                pdfService.extractText(pdfFile);

        List<DocumentChunk> generatedChunks =
                chunkingService.chunkText(text);

        List<ChunkResponse> dtoChunks =
                generatedChunks.stream()
                        .map(chunk ->
                                new ChunkResponse(
                                        fileName,
                                        chunk.chunkNumber(),
                                        chunk.text().length(),
                                        chunk.text()
                                ))
                        .toList();

        return new ChunkingResponse(
                fileName,
                dtoChunks.size(),
                dtoChunks
        );
    }

    @GetMapping("/embeddings/{fileName}")
    @Operation(summary = "Generate embeddings for PDF chunks")
    public EmbeddingBatchResponse generateEmbeddings(
            @PathVariable String fileName)
            throws Exception {

        File pdfFile =
                new File("uploads/" + fileName);

        String text =
                pdfService.extractText(pdfFile);

        List<DocumentChunk> chunks =
                chunkingService.chunkText(text);

        List<EmbeddingResponse> embeddings =
                chunks.stream()
                        .map(chunk ->
                                embeddingService.generateEmbedding(
                                        fileName,
                                        chunk
                                ))
                        .toList();

        return new EmbeddingBatchResponse(
                fileName,
                embeddings.size(),
                embeddings
        );
    }

    @PostMapping("/store/{fileName}")
    @Operation(summary = "Store chunks and embeddings in Chroma")
    public StoreRequestResponse storeInChroma(
            @PathVariable String fileName)
            throws Exception {
        return pdfIngestionService.storePdf(fileName);
    }
}