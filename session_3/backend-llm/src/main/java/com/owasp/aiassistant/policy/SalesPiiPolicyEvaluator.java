package com.owasp.aiassistant.policy;

import com.owasp.aiassistant.corporate.auth.DemoUserRoles;
import com.owasp.aiassistant.corporate.enums.DemoUser;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

@Component
public class SalesPiiPolicyEvaluator {

    private static final String BLOCK_REASON = """
            I can't help reveal redacted customer information. Sales users without the sales-admin role \
            only receive masked customer names and phone numbers from the Sales API.""";

    private static final Pattern UNREDACTED_DEMO_PHONE = Pattern.compile("\\+1-555-\\d");

    private static final List<String> UNREDACTION_PATTERNS = List.of(
            "unredact",
            "deanonymize",
            "de-anonymize",
            "unmask the customer",
            "unmask customer",
            "remove the redaction",
            "remove redaction",
            "remove the asterisks",
            "remove asterisks",
            "decode the asterisks",
            "decode asterisks",
            "reveal the customer",
            "reveal customer",
            "show the real phone",
            "show real phone",
            "show the real name",
            "show real name",
            "actual customer name",
            "actual phone number",
            "real customer name",
            "real phone number",
            "full customer details",
            "bypass the redaction",
            "bypass redaction",
            "uncensor customer",
            "decrypt customer");

    public SalesPiiPolicyEvaluation evaluateInput(String userMessage, DemoUser demoUser) {
        if (DemoUserRoles.canViewSalesCustomerPii(demoUser)) {
            return SalesPiiPolicyEvaluation.allowed();
        }

        if (userMessage == null || userMessage.isBlank()) {
            return SalesPiiPolicyEvaluation.allowed();
        }

        String normalizedMessage = userMessage.toLowerCase(Locale.ROOT);
        for (String pattern : UNREDACTION_PATTERNS) {
            if (normalizedMessage.contains(pattern)) {
                return SalesPiiPolicyEvaluation.blocked(
                        "User attempted to unredact sales customer PII without sales-admin role",
                        BLOCK_REASON);
            }
        }

        return SalesPiiPolicyEvaluation.allowed();
    }

    public boolean containsLeakedSalesPii(String text, DemoUser demoUser) {
        if (DemoUserRoles.canViewSalesCustomerPii(demoUser) || text == null || text.isBlank()) {
            return false;
        }
        return UNREDACTED_DEMO_PHONE.matcher(text).find();
    }
}
