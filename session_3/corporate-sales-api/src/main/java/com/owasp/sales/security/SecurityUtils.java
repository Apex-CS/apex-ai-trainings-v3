package com.owasp.sales.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public final class SecurityUtils {

    private SecurityUtils() {
    }

    public static boolean hasRole(String role) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof AuthenticatedUser user)) {
            return false;
        }
        return user.roles().stream().anyMatch(existingRole -> existingRole.equalsIgnoreCase(role));
    }

    public static boolean canViewSalesCustomerPii() {
        return hasRole("sales-admin");
    }
}
