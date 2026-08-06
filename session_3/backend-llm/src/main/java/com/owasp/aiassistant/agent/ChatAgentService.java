package com.owasp.aiassistant.agent;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.owasp.aiassistant.dto.CodeAttachment;
import com.owasp.aiassistant.exception.AgentHardException;
import com.owasp.aiassistant.guardrails.GuardrailEnforcement;
import com.owasp.aiassistant.guardrails.GuardrailEvaluation;
import com.owasp.aiassistant.guardrails.GuardrailService;
import com.owasp.aiassistant.guardrails.OutputJudgeEvaluation;
import com.owasp.aiassistant.guardrails.OutputJudgeService;
import org.bsc.langgraph4j.CompiledGraph;
import org.bsc.langgraph4j.GraphStateException;
import org.bsc.langgraph4j.NodeOutput;
import org.bsc.langgraph4j.spring.ai.agentexecutor.AgentExecutorEx;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ChatAgentService {

    private static final String SYSTEM_PROMPT = """
            You are an AI assistant for Example Company. You help employees complete tasks and answer questions \
            about finance, IT, marketing, and sales.

            You have access to retrieval tools:
            1. searchWeb - for current public information
            2. queryDatabase / describeDatabaseSchema - for structured company data in PostgreSQL
            3. searchKnowledgeBase - for ingested internal documents (only when RAG is enabled; otherwise it reports that RAG is unavailable)
            4. reviewCode - for security and quality review of user-attached source files (.py, .html, .zip projects)

            When the user attaches code or asks for a code review, call reviewCode before answering.
            For zip attachments, reviewCode expands the archive, shows the project structure, and returns filtered source files.

            Department-specific corporate APIs (Financial, IT, Marketing, Sales) will be available as tools in a future release.

            Decide which tool(s) to use based on the user's question. You may combine tools when needed.
            Always cite whether information came from the database, knowledge base, or web.
            If no tool is needed, answer directly from general business knowledge relevant to the user's department.
            If a tool reports it is unavailable, try another tool before giving up.
            """;

    private final CompiledGraph<AgentExecutorEx.State> workflow;
    private final AgentWarningContext warningContext;
    private final CodeReviewContext codeReviewContext;
    private final ObjectMapper objectMapper;
    private final GuardrailService guardrailService;
    private final OutputJudgeService outputJudgeService;

    public ChatAgentService(
            ChatModel chatModel,
            List<ToolCallback> toolCallbacks,
            AgentWarningContext warningContext,
            CodeReviewContext codeReviewContext,
            ObjectMapper objectMapper,
            GuardrailService guardrailService,
            @Autowired(required = false) OutputJudgeService outputJudgeService) throws GraphStateException {
        this.warningContext = warningContext;
        this.codeReviewContext = codeReviewContext;
        this.objectMapper = objectMapper;
        this.guardrailService = guardrailService;
        this.outputJudgeService = outputJudgeService;
        var graph = AgentExecutorEx.builder()
                .chatModel(chatModel)
                .tools(toolCallbacks)
                .build();
        this.workflow = graph.compile();
    }

    public AgentChatResult chat(String userMessage, String conversationId, CodeAttachment codeToReview) {
        warningContext.clear();
        codeReviewContext.clear();
        try {
            if (codeToReview != null) {
                validateCodeAttachment(codeToReview);
                codeReviewContext.set(codeToReview);
            }

            GuardrailEvaluation inputEvaluation = guardrailService.evaluateInput(userMessage);
            if (inputEvaluation.blocked()) {
                return new AgentChatResult(inputEvaluation.blockReason(), inputEvaluation.softWarnings());
            }

            AgentRunResult runResult = runAgent(userMessage, "", codeToReview);
            List<String> warnings = new ArrayList<>(inputEvaluation.softWarnings());
            String answer = validateOutputWithJudge(userMessage, runResult.answer(), warnings, codeToReview);

            warnings.addAll(guardrailService.collectSoftWarnings(userMessage, answer));
            warnings.addAll(warningContext.drain());

            AgentExecutionTrace executionTrace = enrichExecutionTrace(runResult.executionTrace(), userMessage, answer);
            return new AgentChatResult(answer, List.copyOf(warnings), executionTrace);
        } catch (Exception e) {
            warningContext.clear();
            codeReviewContext.clear();
            throw new AgentHardException("Agent execution failed: " + e.getMessage(), e);
        } finally {
            codeReviewContext.clear();
        }
    }

    private void validateCodeAttachment(CodeAttachment codeToReview) {
        if (!"base64".equalsIgnoreCase(codeToReview.encoding())) {
            throw new IllegalArgumentException("Only base64 encoding is supported for code attachments");
        }
    }

    private String validateOutputWithJudge(
            String userMessage,
            String answer,
            List<String> warnings,
            CodeAttachment codeToReview) throws GraphStateException {
        if (outputJudgeService == null || !outputJudgeService.isEnabled()) {
            return answer;
        }

        int maxAttempts = 1 + outputJudgeService.getMaxRetries();
        String regenerationGuidance = "";

        for (int attempt = 0; attempt < maxAttempts; attempt++) {
            OutputJudgeEvaluation evaluation = outputJudgeService.evaluate(userMessage, answer);

            if (evaluation.passed()) {
                if (evaluation.unavailable()) {
                    warnings.add("Output judge unavailable: " + evaluation.summary());
                }
                return answer;
            }

            if (evaluation.unavailable()) {
                return handleJudgeFailure(answer, evaluation, warnings);
            }

            boolean hasRetryRemaining = attempt + 1 < maxAttempts;
            if (hasRetryRemaining) {
                regenerationGuidance = outputJudgeService.buildRegenerationGuidance(evaluation);
                AgentRunResult runResult = runAgent(userMessage, regenerationGuidance, codeToReview);
                answer = runResult.answer();
                continue;
            }

            return handleJudgeFailure(answer, evaluation, warnings);
        }

        return answer;
    }

    private String handleJudgeFailure(String answer, OutputJudgeEvaluation evaluation, List<String> warnings) {
        if (outputJudgeService.getEnforcement() == GuardrailEnforcement.HARD) {
            warnings.add("Response blocked by output judge: " + formatViolations(evaluation));
            return outputJudgeService.blockedResponseMessage();
        }

        warnings.addAll(evaluation.violations().stream()
                .map(violation -> "Output judge: " + violation)
                .collect(Collectors.toList()));
        if (evaluation.summary() != null && !evaluation.summary().isBlank()) {
            warnings.add("Output judge summary: " + evaluation.summary());
        }
        return answer;
    }

    private static String formatViolations(OutputJudgeEvaluation evaluation) {
        if (evaluation.violations().isEmpty()) {
            return evaluation.summary();
        }
        return String.join("; ", evaluation.violations());
    }

    private AgentRunResult runAgent(
            String userMessage,
            String additionalSystemGuidance,
            CodeAttachment codeToReview) throws GraphStateException {
        List<Message> messages = new ArrayList<>();
        String systemPrompt = buildSystemPrompt(codeToReview);
        if (additionalSystemGuidance != null && !additionalSystemGuidance.isBlank()) {
            systemPrompt += additionalSystemGuidance;
        }
        messages.add(new SystemMessage(systemPrompt));
        messages.add(new UserMessage(userMessage));

        Map<String, Object> inputs = new HashMap<>();
        inputs.put("messages", messages);
        if (codeToReview != null) {
            inputs.put(AgentExecutionTrace.CODE_TO_REVIEW_KEY, serializeCodeToReview(codeToReview));
        }

        List<AgentTraceStep> steps = new ArrayList<>();
        AgentExecutorEx.State finalState = null;

        var stream = workflow.stream(inputs);
        for (NodeOutput<AgentExecutorEx.State> output : stream) {
            finalState = output.state();
            steps.add(new AgentTraceStep(
                    output.node(),
                    System.currentTimeMillis(),
                    AgentMessageTraceMapper.toTraceMessages(finalState.messages()),
                    AgentMessageTraceMapper.extractToolCalls(finalState.messages()),
                    AgentGraphStateMapper.toTraceState(finalState)));
        }

        String answer = finalState == null
                ? "I could not generate a response."
                : finalState.lastMessage()
                        .map(AssistantMessage.class::cast)
                        .map(AssistantMessage::getText)
                        .orElse("I could not generate a response.");

        Map<String, Object> state = finalState == null
                ? initialTraceState()
                : AgentGraphStateMapper.toTraceState(finalState);

        return new AgentRunResult(answer, new AgentExecutionTrace(userMessage, answer, state, List.copyOf(steps)));
    }

    private static Map<String, Object> initialTraceState() {
        return new LinkedHashMap<>();
    }

    private String serializeCodeToReview(CodeAttachment codeToReview) {
        try {
            return objectMapper.writeValueAsString(codeToReview);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Failed to serialize code attachment for graph state", e);
        }
    }

    private String buildSystemPrompt(CodeAttachment codeToReview) {
        String prompt = buildSystemPrompt();
        if (codeToReview == null) {
            return prompt;
        }
        return prompt + "\n\nThe user attached `" + codeToReview.filename()
                + "` for code review. Call reviewCode before answering security or code-quality questions.";
    }

    private static AgentExecutionTrace enrichExecutionTrace(
            AgentExecutionTrace executionTrace,
            String userMessage,
            String answer) {
        if (executionTrace == null) {
            return new AgentExecutionTrace(userMessage, answer, initialTraceState(), List.of());
        }
        return new AgentExecutionTrace(userMessage, answer, executionTrace.state(), executionTrace.steps());
    }

    private String buildSystemPrompt() {
        String guardrailAugmentation = guardrailService.buildSystemPromptAugmentation();
        if (guardrailAugmentation.isBlank()) {
            return SYSTEM_PROMPT;
        }
        return SYSTEM_PROMPT + "\n\n---\n\n# Guardrails\n\n" + guardrailAugmentation;
    }

    private record AgentRunResult(String answer, AgentExecutionTrace executionTrace) {
    }
}
