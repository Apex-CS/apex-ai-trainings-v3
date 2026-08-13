package com.workshop.mcp.module04.security;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;

/**
 * Value object representing the authenticated caller's identity.
 *
 * <p>Extracts identity from the Spring Security context (populated by JWT Bearer validation).
 * NEVER expose the raw token in logs, responses, or tool outputs.
 */
public record CallerIdentity(
        String sub,         // JWT subject — stable user identifier
        String email,       // email claim (safe to log)
        String username,    // preferred_username claim
        String token        // raw Bearer token — for token relay to downstream APIs
) {
    /**
     * Extracts caller identity from the current Spring Security context.
     * Must be called within an authenticated HTTP request.
     */
    public static CallerIdentity fromSecurityContext() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof Jwt jwt)) {
            return new CallerIdentity("anonymous", "anonymous", "anonymous", null);
        }
        return new CallerIdentity(
                jwt.getSubject(),
                jwt.getClaimAsString("email"),
                jwt.getClaimAsString("preferred_username"),
                jwt.getTokenValue()   // raw JWT string — used for token relay
        );
    }

    /**
     * Returns a safe string representation that NEVER includes the token.
     */
    @Override
    public String toString() {
        return "CallerIdentity{sub='%s', email='%s', username='%s'}".formatted(sub, email, username);
    }
}
