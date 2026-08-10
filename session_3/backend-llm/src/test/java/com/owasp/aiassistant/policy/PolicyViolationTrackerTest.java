package com.owasp.aiassistant.policy;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PolicyViolationTrackerTest {

    private PolicyViolationTracker tracker;

    @BeforeEach
    void setUp() {
        tracker = new PolicyViolationTracker();
        tracker.clear();
    }

    @Test
    void summarize_countsSoftAndHardViolations() {
        tracker.recordSoft("Advisory input policy active: blocked-topics", "input-guardrail");
        tracker.recordHard("User attempted to restart app without permissions", "restartAppServer");

        PolicyViolationSummary summary = tracker.summarize();

        assertEquals(1, summary.softCount());
        assertEquals(1, summary.hardCount());
        assertEquals(2, summary.violations().size());
    }

    @Test
    void toStateEntries_includesCountsAndViolationDetails() {
        tracker.recordHard("User attempted to update budget without permissions", "updateBudgetByArea");

        var state = tracker.summarize().toStateEntries();

        assertEquals(0, state.get(PolicyViolationStateKeys.SOFT_COUNT));
        assertEquals(1, state.get(PolicyViolationStateKeys.HARD_COUNT));
        assertTrue(state.get(PolicyViolationStateKeys.VIOLATIONS).toString().contains("update budget"));
    }
}
