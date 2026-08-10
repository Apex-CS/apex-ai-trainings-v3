package com.owasp.aiassistant.policy;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class PolicyViolationTracker {

    private final ThreadLocal<List<PolicyViolation>> violations = ThreadLocal.withInitial(ArrayList::new);

    public void recordSoft(String reason) {
        recordSoft(reason, null);
    }

    public void recordSoft(String reason, String source) {
        if (reason == null || reason.isBlank()) {
            return;
        }
        violations.get().add(new PolicyViolation(PolicyViolationType.SOFT, reason, source));
    }

    public void recordHard(String reason) {
        recordHard(reason, null);
    }

    public void recordHard(String reason, String source) {
        if (reason == null || reason.isBlank()) {
            return;
        }
        violations.get().add(new PolicyViolation(PolicyViolationType.HARD, reason, source));
    }

    public PolicyViolationSummary summarize() {
        List<PolicyViolation> snapshot = List.copyOf(violations.get());
        int softCount = (int) snapshot.stream()
                .filter(violation -> violation.type() == PolicyViolationType.SOFT)
                .count();
        int hardCount = (int) snapshot.stream()
                .filter(violation -> violation.type() == PolicyViolationType.HARD)
                .count();
        return new PolicyViolationSummary(softCount, hardCount, snapshot);
    }

    public void clear() {
        violations.remove();
    }
}
