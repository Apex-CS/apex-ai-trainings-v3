package com.workshop.mcp.module05.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.JsonNode;

import java.util.List;

/**
 * Represents the outcome of a release execution attempt.
 * Uses sealed interface pattern for exhaustive handling.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ReleaseResult(
        String status,
        String projectKey,
        String version,
        String applicationName,
        String environment,
        String message,
        String approvalRequestId,
        String approvalUrl,
        List<JiraIssueDTO> blockers,
        JsonNode deploymentDetails
) {
    public static ReleaseResult blocked(String projectKey, String version, List<JiraIssueDTO> blockers) {
        return new ReleaseResult(
                "BLOCKED", projectKey, version, null, null,
                "Resolve %d critical bug(s) before deploying release %s".formatted(blockers.size(), version),
                null, null, blockers, null);
    }

    public static ReleaseResult pendingApproval(String requestId, String approvalUrl, String message) {
        return new ReleaseResult(
                "PENDING_APPROVAL", null, null, null, null,
                message, requestId, approvalUrl, null, null);
    }

    public static ReleaseResult deployed(String appName, String version, String environment, JsonNode details) {
        return new ReleaseResult(
                "DEPLOYED", null, version, appName, environment,
                "Successfully deployed %s %s to %s".formatted(appName, version, environment),
                null, null, null, details);
    }

    public static ReleaseResult failed(String errorMessage) {
        return new ReleaseResult(
                "FAILED", null, null, null, null,
                errorMessage, null, null, null, null);
    }
}
