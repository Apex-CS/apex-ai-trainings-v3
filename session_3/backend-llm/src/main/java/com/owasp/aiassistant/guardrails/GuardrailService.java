package com.owasp.aiassistant.guardrails;

import com.owasp.aiassistant.config.GuardrailProperties;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

@Service
public class GuardrailService {

    private static final Logger log = LoggerFactory.getLogger(GuardrailService.class);

    private final GuardrailProperties properties;
    private final List<GuardrailPolicySource> policySources;
    private volatile List<GuardrailPolicy> policies = List.of();

    public GuardrailService(GuardrailProperties properties, List<GuardrailPolicySource> policySources) {
        this.properties = properties;
        this.policySources = policySources.stream()
                .sorted(Comparator.comparing(GuardrailPolicySource::getSourceName))
                .toList();
    }

    @PostConstruct
    void loadPolicies() {
        reload();
    }

    public void reload() {
        if (!properties.isEnabled()) {
            policies = List.of();
            log.info("Guardrails are disabled");
            return;
        }

        List<GuardrailPolicy> loaded = new ArrayList<>();
        for (GuardrailPolicySource source : policySources) {
            List<GuardrailPolicy> sourcePolicies = source.loadPolicies();
            loaded.addAll(sourcePolicies);
            log.info("Loaded {} guardrail policies from {}", sourcePolicies.size(), source.getSourceName());
        }
        policies = List.copyOf(loaded);
        log.info("Total guardrail policies loaded: {}", policies.size());
    }

    public List<GuardrailPolicy> getPolicies() {
        return policies;
    }

    public GuardrailEvaluation evaluateInput(String userMessage) {
        if (!properties.isEnabled() || userMessage == null || userMessage.isBlank()) {
            return GuardrailEvaluation.allowed(List.of());
        }

        String normalizedMessage = userMessage.toLowerCase(Locale.ROOT);
        List<String> softWarnings = new ArrayList<>();

        for (GuardrailPolicy policy : policiesOfType(GuardrailType.INPUT)) {
            if (policy.enforcement() == GuardrailEnforcement.HARD) {
                for (String pattern : BlockedTopicsParser.parsePatterns(policy.content())) {
                    if (normalizedMessage.contains(pattern.toLowerCase(Locale.ROOT))) {
                        return GuardrailEvaluation.blocked(
                                "I can't help with that request because it falls outside the assistant's allowed scope.",
                                softWarnings);
                    }
                }
            } else {
                softWarnings.add("Input policy active: " + policy.name());
            }
        }

        return GuardrailEvaluation.allowed(softWarnings);
    }

    public String buildSystemPromptAugmentation() {
        if (!properties.isEnabled()) {
            return "";
        }

        return policies.stream()
                .filter(policy -> policy.type() == GuardrailType.SYSTEM
                        || policy.type() == GuardrailType.TOOL
                        || policy.type() == GuardrailType.OUTPUT)
                .map(this::formatPolicySection)
                .filter(section -> !section.isBlank())
                .collect(Collectors.joining("\n\n"));
    }

    /**
     * Policy context for the output judge: generation policies plus judge-specific rubric.
     * JUDGE policies are not injected into the agent system prompt.
     */
    public String buildJudgePolicyContext() {
        if (!properties.isEnabled()) {
            return "";
        }

        return policies.stream()
                .filter(policy -> policy.type() == GuardrailType.SYSTEM
                        || policy.type() == GuardrailType.TOOL
                        || policy.type() == GuardrailType.OUTPUT
                        || policy.type() == GuardrailType.JUDGE)
                .map(this::formatPolicySection)
                .filter(section -> !section.isBlank())
                .collect(Collectors.joining("\n\n"));
    }

    /**
     * Extension point for non-blocking guardrails from custom sources (e.g. per-user DB policies).
     * Markdown-backed SYSTEM, TOOL, and OUTPUT policies are injected into the system prompt instead.
     */
    @SuppressWarnings("unused")
    public List<String> collectSoftWarnings(String userMessage, String answer) {
        if (!properties.isEnabled()) {
            return List.of();
        }

        List<String> warnings = new ArrayList<>();
        for (GuardrailPolicy policy : policies) {
            if (policy.enforcement() != GuardrailEnforcement.SOFT || policy.type() != GuardrailType.INPUT) {
                continue;
            }
            warnings.add("Advisory input policy active: " + policy.name());
        }
        return List.copyOf(warnings);
    }

    private List<GuardrailPolicy> policiesOfType(GuardrailType type) {
        return policies.stream()
                .filter(policy -> policy.type() == type)
                .toList();
    }

    private String formatPolicySection(GuardrailPolicy policy) {
        String header = "## " + capitalize(policy.type().name()) + " policy: " + policy.name();
        return header + "\n\n" + policy.content().trim();
    }

    private static String capitalize(String value) {
        if (value == null || value.isEmpty()) {
            return value;
        }
        return Character.toUpperCase(value.charAt(0)) + value.substring(1).toLowerCase(Locale.ROOT);
    }
}
