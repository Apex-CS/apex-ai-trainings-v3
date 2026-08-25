package com.acme.aitraining.service;

import com.acme.aitraining.dto.RagResponse;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Service
public class RagService {

  private final ChatClient chatClient;
  private final VectorStore vectorStore;
  private final EmbeddingModel embeddingModel;

  public RagService(
      ChatClient.Builder chatClientBuilder,
      VectorStore vectorStore,
      EmbeddingModel embeddingModel) {

    this.chatClient = chatClientBuilder.build();
    this.vectorStore = vectorStore;
    this.embeddingModel = embeddingModel;
  }

  public RagResponse ask(String question) {

    float[] embedding = embeddingModel.embed(question);

    List<Float> embeddingList =
        IntStream.range(0, embedding.length)
            .mapToObj(i -> embedding[i])
            .toList();

    System.out.println("\n================ QUESTION EMBEDDING ================");
    System.out.println("Embedding dimensions: " + embedding.length);
    System.out.println("First 10 values: "
        + embeddingList.stream().limit(10).toList());
    System.out.println("====================================================\n");

    List<Document> documents =
        vectorStore.similaritySearch(
            SearchRequest.builder()
                .query(question)
                .topK(3)
                .build());

    List<String> retrievedDocuments =
        documents.stream()
            .map(Document::getText)
            .toList();

    String context =
        retrievedDocuments.stream()
            .collect(Collectors.joining("\n\n"));

    System.out.println("\n================ RETRIEVED CONTEXT =================");
    System.out.println(context);
    System.out.println("====================================================\n");

    String prompt = """
            Context:
            %s

            Question:
            %s
            """
        .formatted(context, question);

    System.out.println("\n================ FINAL PROMPT ======================");
    System.out.println(prompt);
    System.out.println("====================================================\n");

    String answer =
        chatClient.prompt()
            .system("""
                    You are an HR assistant.

                    Answer ONLY using the provided context.

                    If the answer exists in the context,
                    provide it directly and concisely.

                    Do not explain your reasoning.

                    Do not say 'based on the provided context'.

                    If the answer is not present, respond:
                    'I could not find that information.'
                    """)
            .user(prompt)
            .call()
            .content();

    return new RagResponse(
        question,
        embeddingList,
        retrievedDocuments,
        prompt,
        answer
    );
  }
}