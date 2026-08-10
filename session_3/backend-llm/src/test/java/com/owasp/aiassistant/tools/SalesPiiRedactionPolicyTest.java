package com.owasp.aiassistant.tools;

import com.owasp.aiassistant.corporate.enums.DemoUser;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SalesPiiRedactionPolicyTest {

    @Test
    void requiresRedactionForSalesUser() {
        assertTrue(SalesPiiRedactionPolicy.requiresRedactionForUser(DemoUser.BART_PEREZ));
        assertFalse(SalesPiiRedactionPolicy.requiresRedactionForUser(DemoUser.SUTANO_DOE));
    }

    @Test
    void alwaysRedactsForNonSalesAdminEvenWhenFlagIsFalse() {
        assertTrue(SalesPiiRedactionPolicy.shouldRedactCustomerPii(false, DemoUser.BART_PEREZ));
    }

    @Test
    void allowsFullPiiForSalesAdminWhenFlagIsFalse() {
        assertFalse(SalesPiiRedactionPolicy.shouldRedactCustomerPii(false, DemoUser.SUTANO_DOE));
    }

    @Test
    void redactsForSalesAdminWhenFlagIsTrue() {
        assertTrue(SalesPiiRedactionPolicy.shouldRedactCustomerPii(true, DemoUser.SUTANO_DOE));
    }
}
