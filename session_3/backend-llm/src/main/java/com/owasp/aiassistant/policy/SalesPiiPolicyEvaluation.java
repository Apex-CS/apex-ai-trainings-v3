package com.owasp.aiassistant.policy;

public record SalesPiiPolicyEvaluation(boolean blocked, String violationReason, String blockReason) {

    public static SalesPiiPolicyEvaluation allowed() {
        return new SalesPiiPolicyEvaluation(false, null, null);
    }

    public static SalesPiiPolicyEvaluation blocked(String violationReason, String blockReason) {
        return new SalesPiiPolicyEvaluation(true, violationReason, blockReason);
    }
}
