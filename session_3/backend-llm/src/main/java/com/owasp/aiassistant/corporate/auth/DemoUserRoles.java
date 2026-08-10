package com.owasp.aiassistant.corporate.auth;

import com.owasp.aiassistant.corporate.enums.DemoUser;

import java.util.List;
import java.util.Map;

public final class DemoUserRoles {

    private static final Map<DemoUser, List<String>> ROLES_BY_USER = Map.of(
            DemoUser.FULANO_SMITH, List.of("financial-admin", "it-user", "marketing-user", "sales-user"),
            DemoUser.SUTANO_DOE, List.of("sales-admin", "financial-user", "it-user", "marketing-user"),
            DemoUser.MENGANA_DAVIDSON, List.of("marketing-admin", "financial-user", "it-user", "sales-user"),
            DemoUser.BART_PEREZ, List.of("it-admin", "financial-user", "marketing-user", "sales-user"));

    private DemoUserRoles() {
    }

    public static List<String> rolesFor(DemoUser demoUser) {
        if (demoUser == null) {
            return List.of();
        }
        return ROLES_BY_USER.getOrDefault(demoUser, List.of());
    }

    public static boolean hasRole(DemoUser demoUser, String role) {
        return rolesFor(demoUser).stream().anyMatch(existingRole -> existingRole.equalsIgnoreCase(role));
    }

    public static boolean canViewSalesCustomerPii(DemoUser demoUser) {
        return hasRole(demoUser, "sales-admin");
    }
}
