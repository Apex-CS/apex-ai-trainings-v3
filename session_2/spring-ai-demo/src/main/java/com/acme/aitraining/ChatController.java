package com.acme.aitraining;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * STEP 1 — The 15-line AI endpoint.
 *
 * Teaching points:
 *  - ChatClient.Builder is auto-configured by whichever model starter is on the classpath.
 *  - This class has ZERO knowledge of which LLM provider is behind it.
 *  - Compare with WebClient: fluent API over a lower-level abstraction (ChatModel).
 */
@RestController
public class ChatController {

    private final ChatClient chatClient;

    ChatController(ChatClient.Builder builder) {
        this.chatClient = builder
                .defaultSystem("You are a Sr Software Engineer expert in Java. Answer in max 3 sentences.")
                .build();
    }

    @GetMapping("/ask")
    String ask(@RequestParam String q) {
        return chatClient.prompt()
                .user(q)
                .call()
                .content();
    }
}
