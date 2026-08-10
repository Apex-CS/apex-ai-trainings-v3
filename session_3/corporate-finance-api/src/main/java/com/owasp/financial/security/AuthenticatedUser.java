package com.owasp.financial.security;

public record AuthenticatedUser(String username, String displayName, java.util.List<String> roles) {
}
