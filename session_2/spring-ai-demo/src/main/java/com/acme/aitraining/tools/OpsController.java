package com.acme.aitraining.tools;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * STEP 4 — Ask: "Is the payments service healthy? Who should I call and
 * what should I tell the customer?" and watch the model chain two tools.
 */
@RestController
public class OpsController {

    private final ChatClient chatClient;
    private final OpsTools opsTools;

    OpsController(ChatClient.Builder builder, OpsTools opsTools) {
        this.opsTools = opsTools;
        this.chatClient = builder
                .defaultSystem("You are an SRE assistant. Use the available tools; never invent statuses.")
                .build();
    }

    @GetMapping("/ops")
    String ops(@RequestParam String q) {
        return chatClient.prompt()
                .user(q)
                .tools(opsTools)
                .call()
                .content();
    }
}
