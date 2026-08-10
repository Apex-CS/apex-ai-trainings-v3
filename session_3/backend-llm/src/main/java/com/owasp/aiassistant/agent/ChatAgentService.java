package com.owasp.aiassistant.agent;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.owasp.aiassistant.corporate.auth.CorporateApiAuthContext;
import com.owasp.aiassistant.corporate.enums.DemoUser;
import com.owasp.aiassistant.dto.CodeAttachment;
import com.owasp.aiassistant.exception.AgentHardException;
import com.owasp.aiassistant.guardrails.GuardrailEnforcement;
import com.owasp.aiassistant.guardrails.GuardrailEvaluation;
import com.owasp.aiassistant.guardrails.GuardrailService;
import com.owasp.aiassistant.guardrails.OutputJudgeEvaluation;
import com.owasp.aiassistant.guardrails.OutputJudgeService;
import com.owasp.aiassistant.policy.CredentialExposurePolicyEvaluation;
import com.owasp.aiassistant.policy.CredentialExposurePolicyEvaluator;
import com.owasp.aiassistant.policy.PolicyViolationTracker;
import com.owasp.aiassistant.policy.SalesPiiPolicyEvaluation;
import com.owasp.aiassistant.policy.SalesPiiPolicyEvaluator;
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

            You also have department corporate API tools (the user's demo JWT determines which calls are allowed):
            5. getBudgetByArea / updateBudgetByArea - Financial API (areas: IT, FINANCE, SALES, MARKETING)
            6. listAppServers / restartAppServer / listAppRestartsByApp - IT operations API (apps: financial-backend, it-backend, sales-backend, marketing-backend)
            7. getProducts / getSales - Sales API product catalog and customer sales records. Always use getSales for \
            transaction-level sales data (not queryDatabase). Pass redactCustomerPii=true unless the chat user has \
            sales-admin; never invent or infer customer names when redacted values contain asterisks.

            When the user attaches code or asks for a code review, call reviewCode before answering.
            For zip attachments, reviewCode expands the archive, shows the project structure, and returns filtered source files.

            Use corporate API tools for live budget data, server catalog, and restart operations.
            If a corporate API returns HTTP 401 or 403, explain that the current demo user lacks permission for that action.

            Decide which tool(s) to use based on the user's question. You may combine tools when needed.
            Always cite whether information came from the database, knowledge base, web, or a corporate API.
            If no tool is needed, answer directly from general business knowledge relevant to the user's department.
            If a tool reports it is unavailable, try another tool before giving up.
            """;

    private final CompiledGraph<AgentExecutorEx.State> workflow;
    private final AgentWarningContext warningContext;
    private final CodeReviewContext codeReviewContext;
    private final ObjectMapper objectMapper;
    private final GuardrailService guardrailService;
    private final OutputJudgeService outputJudgeService;
    private final CorporateApiAuthContext corporateApiAuthContext;
    private final PolicyViolationTracker policyViolationTracker;
    private final SalesPiiPolicyEvaluator salesPiiPolicyEvaluator;
    private final CredentialExposurePolicyEvaluator credentialExposurePolicyEvaluator;

    public ChatAgentService(
            ChatModel chatModel,
            List<ToolCallback> toolCallbacks,
            AgentWarningContext warningContext,
            CodeReviewContext codeReviewContext,
            CorporateApiAuthContext corporateApiAuthContext,
            PolicyViolationTracker policyViolationTracker,
            SalesPiiPolicyEvaluator salesPiiPolicyEvaluator,
            CredentialExposurePolicyEvaluator credentialExposurePolicyEvaluator,
            ObjectMapper objectMapper,
            GuardrailService guardrailService,
            @Autowired(required = false) OutputJudgeService outputJudgeService) throws GraphStateException {
        this.warningContext = warningContext;
        this.codeReviewContext = codeReviewContext;
        this.corporateApiAuthContext = corporateApiAuthContext;
        this.policyViolationTracker = policyViolationTracker;
        this.salesPiiPolicyEvaluator = salesPiiPolicyEvaluator;
        this.credentialExposurePolicyEvaluator = credentialExposurePolicyEvaluator;
        this.objectMapper = objectMapper;
        this.guardrailService = guardrailService;
        this.outputJudgeService = outputJudgeService;
        var graph = AgentExecutorEx.builder()
                .chatModel(chatModel)
                .tools(toolCallbacks)
                .build();
        this.workflow = graph.compile();
    }

    public AgentChatResult chat(String userMessage, String conversationId, CodeAttachment codeToReview, DemoUser demoUser) {
        warningContext.clear();
        codeReviewContext.clear();
        policyViolationTracker.clear();
        corporateApiAuthContext.set(demoUser);
        try {
            if (codeToReview != null) {
                validateCodeAttachment(codeToReview);
                codeReviewContext.set(codeToReview);
            }

            GuardrailEvaluation inputEvaluation = guardrailService.evaluateInput(userMessage);
            recordInputPolicyViolations(inputEvaluation);
            if (inputEvaluation.blocked()) {
                return buildResult(
                        inputEvaluation.blockReason(),
                        inputEvaluation.softWarnings(),
                        userMessage,
                        inputEvaluation.blockReason());
            }

            SalesPiiPolicyEvaluation salesPiiEvaluation = salesPiiPolicyEvaluator.evaluateInput(userMessage, demoUser);
            if (salesPiiEvaluation.blocked()) {
                policyViolationTracker.recordHard(salesPiiEvaluation.violationReason(), "sales-pii-policy");
                return buildResult(
                        salesPiiEvaluation.blockReason(),
                        inputEvaluation.softWarnings(),
                        userMessage,
                        salesPiiEvaluation.blockReason());
            }

            CredentialExposurePolicyEvaluation credentialEvaluation =
                    credentialExposurePolicyEvaluator.evaluate(userMessage, codeToReview);
            if (credentialEvaluation.blocked()) {
                policyViolationTracker.recordHard(
                        credentialEvaluation.violationReason(),
                        "credential-exposure-policy");
                return buildResult(
                        credentialEvaluation.blockReason(),
                        inputEvaluation.softWarnings(),
                        userMessage,
                        credentialEvaluation.blockReason());
            }

            AgentRunResult runResult = runAgent(userMessage, "", codeToReview);
            List<String> warnings = new ArrayList<>(inputEvaluation.softWarnings());
            String answer = validateOutputWithJudge(userMessage, runResult.answer(), warnings, codeToReview);
            recordSalesPiiOutputViolations(answer, demoUser);

            List<String> advisoryWarnings = guardrailService.collectSoftWarnings(userMessage, answer);
            warnings.addAll(advisoryWarnings);
            recordSoftWarnings(advisoryWarnings, "advisory-guardrail");
            warnings.addAll(warningContext.drain());

            return buildResult(answer, warnings, runResult.executionTrace());
        } catch (Exception e) {
            warningContext.clear();
            codeReviewContext.clear();
            policyViolationTracker.clear();
            corporateApiAuthContext.clear();
            throw new AgentHardException("Agent execution failed: " + e.getMessage(), e);
        } finally {
            codeReviewContext.clear();
            corporateApiAuthContext.clear();
            policyViolationTracker.clear();
        }
    }

    private void recordInputPolicyViolations(GuardrailEvaluation inputEvaluation) {
        if (inputEvaluation.blocked()) {
            policyViolationTracker.recordHard(
                    "User input blocked by policy: " + inputEvaluation.blockReason(),
                    "input-guardrail");
        }
        recordSoftWarnings(inputEvaluation.softWarnings(), "input-guardrail");
    }

    private void recordSoftWarnings(List<String> warnings, String source) {
        for (String warning : warnings) {
            policyViolationTracker.recordSoft(warning, source);
        }
    }

    private void recordSalesPiiOutputViolations(String answer, DemoUser demoUser) {
        if (salesPiiPolicyEvaluator.containsLeakedSalesPii(answer, demoUser)) {
            policyViolationTracker.recordHard(
                    "Assistant revealed redacted sales customer PII without sales-admin role",
                    "sales-pii-policy");
        }
    }

    private AgentChatResult buildResult(
            String answer,
            List<String> warnings,
            String userMessage,
            String assistantAnswer) {
        AgentExecutionTrace trace = new AgentExecutionTrace(
                userMessage,
                assistantAnswer,
                mergePolicyViolationsIntoState(new LinkedHashMap<>()),
                List.of());
        return new AgentChatResult(answer, List.copyOf(warnings), trace);
    }

    private AgentChatResult buildResult(
            String answer,
            List<String> warnings,
            AgentExecutionTrace executionTrace) {
        if (executionTrace == null) {
            return buildResult(answer, warnings, "", answer);
        }
        AgentExecutionTrace enrichedTrace = new AgentExecutionTrace(
                executionTrace.userMessage(),
                executionTrace.assistantAnswer(),
                mergePolicyViolationsIntoState(executionTrace.state()),
                executionTrace.steps());
        return new AgentChatResult(answer, List.copyOf(warnings), enrichedTrace);
    }

    private Map<String, Object> mergePolicyViolationsIntoState(Map<String, Object> state) {
        Map<String, Object> enrichedState = new LinkedHashMap<>(state);
        enrichedState.putAll(policyViolationTracker.summarize().toStateEntries());
        return enrichedState;
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
                    policyViolationTracker.recordSoft(
                            "Output judge unavailable: " + evaluation.summary(),
                            "output-judge");
                }
                return answer;
            }

            if (evaluation.unavailable()) {
                policyViolationTracker.recordSoft(
                        "Output judge unavailable: " + evaluation.summary(),
                        "output-judge");
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
            String reason = "Response blocked by output judge: " + formatViolations(evaluation);
            warnings.add(reason);
            policyViolationTracker.recordHard(reason, "output-judge");
            return outputJudgeService.blockedResponseMessage();
        }

        evaluation.violations().forEach(violation ->
                policyViolationTracker.recordSoft("Output judge: " + violation, "output-judge"));
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
