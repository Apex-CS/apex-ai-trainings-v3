package com.acme.pdfingestion.controller;

import com.acme.pdfingestion.dto.ChromaCollectionSummaryResponse;
import com.acme.pdfingestion.service.ChromaExplorerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.ai.chroma.vectorstore.ChromaApi;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/chroma")
@Tag(name = "Chroma Explorer")
public class ChromaController {

    private final ChromaExplorerService chromaExplorerService;

    public ChromaController(
            ChromaExplorerService chromaExplorerService) {

        this.chromaExplorerService =
                chromaExplorerService;
    }

    @GetMapping("/collections")
    @Operation(summary = "List all collections")
    public List<ChromaCollectionSummaryResponse> listCollections() {

        return chromaExplorerService.listCollections();
    }

    @GetMapping("/collections/{collection}")
    @Operation(summary = "Get collection details")
    public ChromaApi.Collection getCollection(
            @PathVariable String collection) {

        return chromaExplorerService.getCollection(
                collection
        );
    }

    @GetMapping("/collections/{collection}/count")
    @Operation(summary = "Get collection record count")
    public Long countRecords(
            @PathVariable String collection) {

        return chromaExplorerService.countRecords(
                collection
        );
    }
}