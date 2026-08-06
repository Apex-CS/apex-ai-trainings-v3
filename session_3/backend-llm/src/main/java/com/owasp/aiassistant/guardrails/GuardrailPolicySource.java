package com.owasp.aiassistant.guardrails;

import java.util.List;

/**
 * Loads guardrail policies from a backing store (classpath markdown, database, etc.).
 * <p>
 * Register additional implementations as Spring beans to combine multiple sources.
 * For example, a future {@code DatabaseGuardrailPolicySource} could return per-user
 * {@link GuardrailEnforcement#SOFT} policies while markdown files provide the baseline.
 */
public interface GuardrailPolicySource {

    String getSourceName();

    List<GuardrailPolicy> loadPolicies();
}
