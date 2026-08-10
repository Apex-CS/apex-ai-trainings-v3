package com.owasp.aiassistant.policy;

public record CredentialExposurePolicyEvaluation(boolean blocked, String violationReason, String blockReason) {

    public static CredentialExposurePolicyEvaluation allowed() {
        return new CredentialExposurePolicyEvaluation(false, null, null);
    }

    public static CredentialExposurePolicyEvaluation blocked(String violationReason, String blockReason) {
        return new CredentialExposurePolicyEvaluation(true, violationReason, blockReason);
    }
}
