package com.workshop.mcp.module04.web;

import com.workshop.mcp.module04.audit.AuditLogService;
import com.workshop.mcp.module04.security.CallerIdentity;
import com.workshop.mcp.module04.security.HumanInTheLoopGuard;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Human-in-the-Loop approval REST endpoint.
 *
 * <p>When a tool call requires human approval (e.g. PROD deployment),
 * the tool returns a requestId and approvalUrl. A human navigates to
 * POST /confirm/{requestId} with their Bearer token to authorize the action.
 *
 * <p>In production, this endpoint would be a UI page, not a raw REST endpoint.
 * It requires authentication to prevent anonymous approvals.
 */
@RestController
@RequestMapping("/confirm")
public class ApprovalController {

    private final HumanInTheLoopGuard humanGuard;
    private final AuditLogService auditLog;

    public ApprovalController(HumanInTheLoopGuard humanGuard, AuditLogService auditLog) {
        this.humanGuard = humanGuard;
        this.auditLog = auditLog;
    }

    @PostMapping("/{requestId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, Object>> approve(@PathVariable String requestId) {
        var approver = CallerIdentity.fromSecurityContext();

        try {
            var approval = humanGuard.approve(requestId, approver.username());
            auditLog.approvalGranted(requestId, approver.username());

            return ResponseEntity.ok(Map.of(
                    "approved", true,
                    "requestId", requestId,
                    "description", approval.description(),
                    "approvedBy", approver.email(),
                    "message", "Deployment approved. It will execute on the next LLM retry."));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}
