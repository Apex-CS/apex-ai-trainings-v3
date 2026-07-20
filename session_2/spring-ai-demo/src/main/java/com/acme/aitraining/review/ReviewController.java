package com.acme.aitraining.review;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * STEP 2 — Type safety over an LLM.
 *
 * Teaching points:
 *  - .entity(CodeReview.class) replaces .content(): schema in, typed object out.
 *  - For extraction/classification tasks lower the temperature (Topic 1 callback).
 *  - No JSON parsing code anywhere. This is the "Spring Data moment" of the talk.
 */
@RestController
public class ReviewController {

    private final ChatClient chatClient;

    ReviewController(ChatClient.Builder builder) {
        this.chatClient = builder
                .defaultSystem("""
                        You are a strict senior Java code reviewer.
                        Focus on correctness, resource handling, naming and thread safety.
                        """)
                .build();
    }

    @PostMapping("/review")
    CodeReview review(@RequestBody String code) {
        return chatClient.prompt()
                .user(u -> u.text("Review this Java code:\n\n{code}").param("code", code))
                .call()
                .entity(CodeReview.class);
    }
}
