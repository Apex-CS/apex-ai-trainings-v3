package com.owasp.aiassistant.web;

import com.owasp.aiassistant.agent.AgentChatResult;
import com.owasp.aiassistant.agent.ChatAgentService;
import com.owasp.aiassistant.corporate.enums.DemoUser;
import com.owasp.aiassistant.dto.ChatRequest;
import com.owasp.aiassistant.dto.ChatResponse;
import com.owasp.aiassistant.mlflow.MlflowChatTracingService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/chat")
public class ChatController {

    private final ChatAgentService chatAgentService;
    private final MlflowChatTracingService mlflowChatTracingService;

    public ChatController(
            ChatAgentService chatAgentService,
            @Autowired(required = false) MlflowChatTracingService mlflowChatTracingService) {
        this.chatAgentService = chatAgentService;
        this.mlflowChatTracingService = mlflowChatTracingService;
    }

    @PostMapping
    public ResponseEntity<ChatResponse> chat(@Valid @RequestBody ChatRequest request) {
        String conversationId = request.conversationId() != null
                ? request.conversationId()
                : UUID.randomUUID().toString();

        long startTimeMs = System.currentTimeMillis();
        try {
            AgentChatResult result = chatAgentService.chat(
                    request.message(),
                    conversationId,
                    request.codeToReview(),
                    request.demoUser());
            recordChatTurn(conversationId, request.message(), result, System.currentTimeMillis() - startTimeMs, request.demoUser());
            return ResponseEntity.ok(new ChatResponse(result.answer(), conversationId, result.warnings()));
        } catch (Exception e) {
            recordChatError(conversationId, request.message(), System.currentTimeMillis() - startTimeMs, e, request.demoUser());
            throw e;
        }
    }

    private void recordChatTurn(
            String conversationId,
            String userMessage,
            AgentChatResult result,
            long durationMs,
            DemoUser demoUser) {
        if (mlflowChatTracingService != null) {
            mlflowChatTracingService.recordChatTurn(conversationId, userMessage, result, durationMs, demoUser);
        }
    }

    private void recordChatError(
            String conversationId,
            String userMessage,
            long durationMs,
            Exception error,
            DemoUser demoUser) {
        if (mlflowChatTracingService != null) {
            mlflowChatTracingService.recordChatError(conversationId, userMessage, durationMs, error, demoUser);
        }
    }
}
