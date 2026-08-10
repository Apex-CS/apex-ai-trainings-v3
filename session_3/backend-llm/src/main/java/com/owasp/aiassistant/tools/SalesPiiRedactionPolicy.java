package com.owasp.aiassistant.tools;

import com.owasp.aiassistant.corporate.auth.DemoUserRoles;
import com.owasp.aiassistant.corporate.enums.DemoUser;

public final class SalesPiiRedactionPolicy {

    private SalesPiiRedactionPolicy() {
    }

    public static boolean shouldRedactCustomerPii(boolean redactCustomerPii, DemoUser demoUser) {
        return !DemoUserRoles.canViewSalesCustomerPii(demoUser) || redactCustomerPii;
    }

    public static boolean requiresRedactionForUser(DemoUser demoUser) {
        return !DemoUserRoles.canViewSalesCustomerPii(demoUser);
    }
}
