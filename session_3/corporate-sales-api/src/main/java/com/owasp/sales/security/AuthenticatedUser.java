package com.owasp.sales.security;

public record AuthenticatedUser(String username, String displayName, java.util.List<String> roles) {
}
