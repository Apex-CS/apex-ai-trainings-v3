package com.acme.aitraining.service;

import jakarta.annotation.PostConstruct;
import org.springframework.ai.chroma.vectorstore.ChromaApi;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class DocumentIngestionService {

  private final ChromaApi chromaApi;
  private final EmbeddingModel embeddingModel;

  private static final String TENANT = "default_tenant";
  private static final String DATABASE = "default_database";
  private static final String COLLECTION = "demo-rag";

  public DocumentIngestionService(
          ChromaApi chromaApi,
          EmbeddingModel embeddingModel) {

    this.chromaApi = chromaApi;
    this.embeddingModel = embeddingModel;
  }

  @PostConstruct
  public void loadDocuments() {

    String text = """
                Employee Handbook

                Vacation Policy

                Employees receive 20 PTO days annually.

                Employees may carry over up to 5 PTO days into the following year.

                Remote Work Policy

                Employees may work remotely up to three days per week.

                Travel Policy

                Manager approval is required before any business travel is booked.
                """;

    Document document = new Document(text);

    System.out.println("\n================ SOURCE DOCUMENT ===================");
    System.out.println(document.getText());
    System.out.println("====================================================\n");

    float[] embedding =
            embeddingModel.embed(document.getText());

    System.out.println("\n=============== DOCUMENT EMBEDDING =================");
    System.out.println("Embedding dimensions: " + embedding.length);

    System.out.println("First 10 values:");

    for (int i = 0; i < Math.min(10, embedding.length); i++) {
      System.out.printf("[%d] = %.8f%n", i, embedding[i]);
    }

    System.out.println("====================================================\n");

    ChromaApi.Collection collection =
            chromaApi.getCollection(
                    TENANT,
                    DATABASE,
                    COLLECTION
            );

    String collectionId = collection.id();

    System.out.println("\n================ CHROMA COLLECTION =================");
    System.out.println("Collection Name : " + collection.name());
    System.out.println("Collection Id   : " + collectionId);
    System.out.println("====================================================\n");

    ChromaApi.AddEmbeddingsRequest request =
            new ChromaApi.AddEmbeddingsRequest(
                    "employee-handbook-1",
                    embedding,
                    Map.of(
                            "source", "employee-handbook",
                            "type", "policy"
                    ),
                    document.getText()
            );

    chromaApi.upsertEmbeddings(
            TENANT,
            DATABASE,
            collectionId,
            request
    );

    System.out.println("\n================ DOCUMENT STORED ===================");
    System.out.println("Collection : " + COLLECTION);
    System.out.println("Collection Id: " + collectionId);
    System.out.println("Document Id: employee-handbook-1");
    System.out.println("====================================================\n");
  }
}