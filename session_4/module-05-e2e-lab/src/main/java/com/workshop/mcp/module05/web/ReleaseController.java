package com.workshop.mcp.module05.web;

import com.workshop.mcp.module05.agent.ReleaseIntegrationAgent;
import com.workshop.mcp.module05.dto.ReleaseResult;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Release endpoint — the entry point for the Enterprise Release Agent.
 *
 * <p>Requires OAuth2 Bearer token authentication. The token is extracted
 * by Spring Security and injected as a Jwt principal for token relay.
 */
@RestController
@RequestMapping("/release")
public class ReleaseController {

    private final ReleaseIntegrationAgent agent;

    public ReleaseController(ReleaseIntegrationAgent agent) {
        this.agent = agent;
    }

    @PostMapping
    public ResponseEntity<ReleaseResult> release(
            @RequestBody ReleaseRequest request,
            @AuthenticationPrincipal Jwt jwt) {

        // Token relay: pass the caller's raw JWT string to the agent
        // The agent forwards it to the Deployment MCP Server for authentication
        String bearerToken = jwt.getTokenValue();

        ReleaseResult result = agent.executeRelease(
                request.projectKey(),
                request.version(),
                request.applicationName(),
                request.environment(),
                bearerToken);

        // Map result status to HTTP status code
        int httpStatus = switch (result.status()) {
            case "DEPLOYED"          -> 200;
            case "PENDING_APPROVAL"  -> 202;  // Accepted — awaiting human approval
            case "BLOCKED"           -> 409;  // Conflict — critical bugs block release
            default                  -> 500;
        };

        return ResponseEntity.status(httpStatus).body(result);
    }

    public record ReleaseRequest(
            String projectKey,
            String version,
            String applicationName,
            String environment
    ) {}
}
