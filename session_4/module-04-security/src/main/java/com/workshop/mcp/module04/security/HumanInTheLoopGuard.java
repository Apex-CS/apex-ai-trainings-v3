package com.workshop.mcp.module04.security;

import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Human-in-the-Loop Guard — Pending Approval State Machine.
 *
 * <p>For high-impact tool calls (e.g. deploying to PROD, deleting a repository),
 * the guard suspends execution and creates a PendingApproval record. Execution
 * continues only after a human explicitly approves via the /confirm/{requestId} endpoint.
 *
 * <p>This implements the principle of least privilege for LLM agents: the agent
 * can PROPOSE an action to PROD, but a human must AUTHORIZE it.
 *
 * <p><strong>Production note:</strong> Use Redis or a database for the pending map
 * to survive restarts and support multi-instance deployments.
 */
@Component
public class HumanInTheLoopGuard {

    private final Map<String, PendingApproval> pending = new ConcurrentHashMap<>();

    /**
     * Creates a pending approval record and returns its unique ID.
     * The caller should return the requestId to the LLM, which will display
     * it to the user as an action item.
     */
    public String requireApproval(String description, String requestedBy) {
        String requestId = UUID.randomUUID().toString();
        pending.put(requestId, new PendingApproval(
                requestId,
                description,
                requestedBy,
                Instant.now(),
                ApprovalStatus.PENDING,
                null));
        return requestId;
    }

    /**
     * Returns true if the specified request has been explicitly approved by a human.
     */
    public boolean isApproved(String requestId) {
        var approval = pending.get(requestId);
        return approval != null && approval.status() == ApprovalStatus.APPROVED;
    }

    /**
     * Approves the pending request. Called by the /confirm/{requestId} REST endpoint.
     *
     * @param requestId  the unique approval request identifier
     * @param approvedBy the identity of the human who approved
     */
    public PendingApproval approve(String requestId, String approvedBy) {
        var existing = pending.get(requestId);
        if (existing == null) {
            throw new IllegalArgumentException("Unknown approval request: " + requestId);
        }
        if (existing.status() == ApprovalStatus.APPROVED) {
            throw new IllegalStateException("Request " + requestId + " is already approved");
        }
        var approved = new PendingApproval(
                existing.requestId(),
                existing.description(),
                existing.requestedBy(),
                existing.createdAt(),
                ApprovalStatus.APPROVED,
                approvedBy);
        pending.put(requestId, approved);
        return approved;
    }

    public PendingApproval getPending(String requestId) {
        return pending.get(requestId);
    }

    // ─── Domain types ─────────────────────────────────────────────────────────

    public record PendingApproval(
            String requestId,
            String description,
            String requestedBy,
            Instant createdAt,
            ApprovalStatus status,
            String approvedBy
    ) {}

    public enum ApprovalStatus {
        PENDING,
        APPROVED,
        REJECTED
    }
}
