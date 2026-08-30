package com.acme.aitraining.controller;

import com.acme.aitraining.dto.FastShowQueryRequest;
import com.acme.aitraining.dto.FastShowQueryResponse;
import com.acme.aitraining.dto.FastShowUploadResponse;
import com.acme.aitraining.service.FastShowService;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import io.swagger.v3.oas.annotations.Parameter;

@RestController
@RequestMapping("/api/fast-show")
public class FastShowController {

    private final FastShowService fastShowService;

    public FastShowController(
            FastShowService fastShowService) {

        this.fastShowService = fastShowService;
    }

    @PostMapping(
            value = "/upload",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    @Operation(
            summary = "Upload multiple PDF files into FastShow collection"
    )
    public FastShowUploadResponse upload(

            @Parameter(
                    description = "One or more PDF files"
            )
            @RequestPart("files")
            MultipartFile[] files)
            throws Exception {

        return fastShowService.uploadPdfs(files);
    }

    @PostMapping("/query")
    @Operation(
            summary = "Query all uploaded PDF files using RAG"
    )
    public FastShowQueryResponse query(
            @RequestBody
            FastShowQueryRequest request)
            throws Exception {

        return fastShowService.query(
                request.question()
        );
    }
}