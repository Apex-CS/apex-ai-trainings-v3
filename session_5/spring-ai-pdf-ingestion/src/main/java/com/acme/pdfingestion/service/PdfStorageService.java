package com.acme.pdfingestion.service;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Service
public class PdfStorageService {

    private static final Path STORAGE_FOLDER =
            Path.of("uploads");

    public String store(MultipartFile file)
            throws IOException {

        if (!Files.exists(STORAGE_FOLDER)) {
            Files.createDirectories(STORAGE_FOLDER);
        }

        String documentId =
                UUID.randomUUID().toString();

        String fileName =
                documentId + "-" + file.getOriginalFilename();

        Path destination =
                STORAGE_FOLDER.resolve(fileName);

        Files.copy(
                file.getInputStream(),
                destination,
                StandardCopyOption.REPLACE_EXISTING
        );

        return fileName;
    }
}