package com.acme.aitraining.service;

import com.acme.aitraining.dto.FastShowQueryResponse;
import com.acme.aitraining.dto.FastShowUploadResponse;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chroma.vectorstore.ChromaApi;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Service
public class FastShowService {

    private static final String TENANT =
            "default_tenant";

    private static final String DATABASE =
            "default_database";

    private static final String COLLECTION_NAME =
            "FastShow-collection";

    private static final int CHUNK_SIZE =
            1000;

    private static final int CHUNK_OVERLAP =
            200;

    private final ChromaApi chromaApi;
    private final EmbeddingModel embeddingModel;
    private final VectorStore fastShowVectorStore;
    private final ChatClient chatClient;

    public FastShowService(
            ChromaApi chromaApi,
            EmbeddingModel embeddingModel,
            @Qualifier("fastShowVectorStore")
            VectorStore fastShowVectorStore,
            ChatClient.Builder chatClientBuilder) {

        this.chromaApi = chromaApi;
        this.embeddingModel = embeddingModel;
        this.fastShowVectorStore = fastShowVectorStore;
        this.chatClient = chatClientBuilder.build();
    }

    public FastShowUploadResponse uploadPdfs(
            MultipartFile[] files)
            throws Exception {

        System.out.println("\n====================================================");
        System.out.println("FAST SHOW - PDF INGESTION");
        System.out.println("====================================================");

        ChromaApi.Collection collection =
                getOrCreateCollection();

        String collectionId =
                collection.id();

        System.out.println("Collection Name : "
                + collection.name());

        System.out.println("Collection Id   : "
                + collectionId);

        int totalChunks = 0;

        List<String> uploadedFiles =
                new ArrayList<>();

        for (MultipartFile file : files) {

            String fileName =
                    Objects.requireNonNull(
                            file.getOriginalFilename());

            uploadedFiles.add(fileName);

            System.out.println(
                    "\n================ PDF FILE ==========================");
            System.out.println(fileName);
            System.out.println("====================================================");

            String text =
                    extractText(file);

            System.out.println(
                    "Characters Extracted : "
                            + text.length());

            List<String> chunks =
                    splitIntoChunks(text);

            totalChunks += chunks.size();

            System.out.println(
                    "Chunks Created       : "
                            + chunks.size());

            for (int i = 0; i < chunks.size(); i++) {

                String chunkText =
                        chunks.get(i);

                float[] embedding =
                        embeddingModel.embed(chunkText);

                String documentId =
                        fileName
                                .replace(" ", "_")
                                .replace(".pdf", "")
                                + "-chunk-"
                                + (i + 1)
                                + "-"
                                + UUID.randomUUID();

                ChromaApi.AddEmbeddingsRequest request =
                        new ChromaApi.AddEmbeddingsRequest(
                                documentId,
                                embedding,
                                Map.of(
                                        "sourceFile",
                                        fileName,
                                        "chunkNumber",
                                        i + 1
                                ),
                                chunkText
                        );

                chromaApi.upsertEmbeddings(
                        TENANT,
                        DATABASE,
                        collectionId,
                        request
                );
            }

            System.out.println(
                    "Embeddings Stored    : "
                            + chunks.size());
        }

        System.out.println("\n====================================================");
        System.out.println("INGESTION COMPLETE");
        System.out.println("Files Uploaded : " + files.length);
        System.out.println("Chunks Created : " + totalChunks);
        System.out.println("====================================================\n");

        return new FastShowUploadResponse(
                COLLECTION_NAME,
                files.length,
                totalChunks,
                totalChunks,
                uploadedFiles,
                "SUCCESS"
        );
    }

    public FastShowQueryResponse query(
            String question) {

        System.out.println("\n================ QUESTION ==========================");
        System.out.println(question);
        System.out.println("====================================================\n");

        float[] embedding =
                embeddingModel.embed(question);

        List<Float> embeddingList =
                IntStream.range(0, embedding.length)
                        .mapToObj(i -> embedding[i])
                        .toList();

        System.out.println(
                "Question Embedding Dimensions : "
                        + embedding.length);

        System.out.println(
                "First 10 Values : "
                        + embeddingList.stream()
                        .limit(10)
                        .toList());

        List<Document> documents =
                fastShowVectorStore.similaritySearch(
                        SearchRequest.builder()
                                .query(question)
                                .topK(5)
                                .build()
                );

        System.out.println(
                "\n================ RETRIEVED CHUNKS =================");

        System.out.println(
                "Chunks Retrieved : "
                        + documents.size());

        documents.forEach(document ->
                System.out.println(
                        "Source : "
                                + document.getMetadata()
                                .get("sourceFile")));

        System.out.println(
                "====================================================\n");

        List<String> retrievedChunks =
                documents.stream()
                        .map(Document::getText)
                        .toList();

        List<String> sources =
                documents.stream()
                        .map(document ->
                                Objects.toString(
                                        document.getMetadata()
                                                .get("sourceFile")
                                ))
                        .distinct()
                        .collect(Collectors.toList());

        String context =
                IntStream.range(0, documents.size())
                        .mapToObj(i -> {

                            Document document =
                                    documents.get(i);

                            return """
                                    Source:
                                    %s

                                    Content:
                                    %s
                                    """
                                    .formatted(
                                            document.getMetadata()
                                                    .get("sourceFile"),
                                            document.getText()
                                    );
                        })
                        .collect(Collectors.joining("\n\n"));

        System.out.println(
                "\n================ RETRIEVED CONTEXT ================");

        System.out.println(context);

        System.out.println(
                "====================================================\n");

        String prompt = """
                Context:
                %s

                Question:
                %s
                """
                .formatted(
                        context,
                        question
                );

        System.out.println(
                "\n================ FINAL PROMPT =====================");

        System.out.println(prompt);

        System.out.println(
                "====================================================\n");

        String answer =
                chatClient.prompt()
                        .system("""
                                You are a retrieval assistant.

                                Answer ONLY using the provided context.

                                Do not use external knowledge.

                                If the answer is not present in the context,
                                respond exactly:

                                I could not find that information.

                                Do not invent information.
                                """)
                        .user(prompt)
                        .call()
                        .content();

        System.out.println(
                "\n================ FINAL ANSWER =====================");

        System.out.println(answer);

        System.out.println(
                "====================================================\n");

        return new FastShowQueryResponse(
                question,
                retrievedChunks,
                sources,
                prompt,
                answer
        );
    }

    private ChromaApi.Collection getOrCreateCollection() {

        try {

            return chromaApi.getCollection(
                    TENANT,
                    DATABASE,
                    COLLECTION_NAME
            );

        } catch (Exception ex) {

            System.out.println(
                    "Collection does not exist. Creating collection: "
                            + COLLECTION_NAME);

            return chromaApi.createCollection(
                    TENANT,
                    DATABASE,
                    new ChromaApi.CreateCollectionRequest(
                            COLLECTION_NAME
                    )
            );
        }
    }

    private String extractText(
            MultipartFile file)
            throws Exception {

        try (PDDocument document =
                     Loader.loadPDF(
                             file.getBytes())) {

            PDFTextStripper stripper =
                    new PDFTextStripper();

            return stripper.getText(document);
        }
    }

    private List<String> splitIntoChunks(
            String text) {

        List<String> chunks =
                new ArrayList<>();

        int start = 0;

        while (start < text.length()) {

            int end =
                    Math.min(
                            start + CHUNK_SIZE,
                            text.length()
                    );

            chunks.add(
                    text.substring(start, end)
            );

            start +=
                    (CHUNK_SIZE - CHUNK_OVERLAP);
        }

        return chunks;
    }
}