package com.owasp.aiassistant.corporate.client;

public final class CorporateApiPolicyMessages {

    private CorporateApiPolicyMessages() {
    }

    public static String forbiddenToolMessage(String toolName) {
        return switch (toolName) {
            case "restartAppServer" -> "User attempted to restart app without permissions";
            case "updateBudgetByArea" -> "User attempted to update budget without permissions";
            case "listAppRestartsByApp" -> "User attempted to list app restarts without permissions";
            case "getBudgetByArea" -> "User attempted to get budget data without permissions";
            case "listAppServers" -> "User attempted to list app servers without permissions";
            case "getProducts" -> "User attempted to list sales products without permissions";
            case "getSales" -> "User attempted to get customer PII from sales records without permissions";
            default -> "User attempted to execute " + toolName + " without permission";
        };
    }
}
