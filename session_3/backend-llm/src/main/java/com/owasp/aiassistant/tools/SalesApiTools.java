package com.owasp.aiassistant.tools;

import com.owasp.aiassistant.corporate.auth.CorporateApiAuthContext;
import com.owasp.aiassistant.corporate.client.CorporateApiClient;
import com.owasp.aiassistant.corporate.enums.DemoUser;
import com.owasp.aiassistant.corporate.enums.SalesProductCode;
import com.owasp.aiassistant.policy.PolicyViolationTracker;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class SalesApiTools {

    private static final String GET_PRODUCTS_TOOL = "getProducts";
    private static final String GET_SALES_TOOL = "getSales";

    private final CorporateApiClient corporateApiClient;
    private final CorporateApiAuthContext corporateApiAuthContext;
    private final SalesRecordRedactor salesRecordRedactor;
    private final PolicyViolationTracker policyViolationTracker;

    public SalesApiTools(
            CorporateApiClient corporateApiClient,
            CorporateApiAuthContext corporateApiAuthContext,
            SalesRecordRedactor salesRecordRedactor,
            PolicyViolationTracker policyViolationTracker) {
        this.corporateApiClient = corporateApiClient;
        this.corporateApiAuthContext = corporateApiAuthContext;
        this.salesRecordRedactor = salesRecordRedactor;
        this.policyViolationTracker = policyViolationTracker;
    }

    @Tool(description = """
            List available rubber duck products from the Sales API catalog.
            Requires the chat user to have sales-admin or sales-user role.
            """)
    public String getProducts() {
        return corporateApiClient.get(
                GET_PRODUCTS_TOOL,
                corporateApiClient.salesBaseUrl(),
                "/api/get-products",
                Map.of());
    }

    @Tool(description = """
            Retrieve sales records from the Sales API.
            Always use this tool for customer sales questions; do not use queryDatabase for sales transactions.
            Set redactCustomerPii=true when the chat user does NOT have the sales-admin role.
            When redactCustomerPii=true, customer names and phones are masked with asterisks before returning.
            Optionally filter by SalesProductCode.
            """)
    public String getSales(
            @ToolParam(description = "Optional product code filter", required = false)
            SalesProductCode product,
            @ToolParam(description = """
                    Set true when the chat user lacks sales-admin role so customer names and phones are redacted \
                    with asterisks. Set false only for sales-admin users.""")
            boolean redactCustomerPii) {
        Map<String, String> queryParams = new LinkedHashMap<>();
        if (product != null) {
            queryParams.put("product", product.apiValue());
        }

        String salesResponse = corporateApiClient.get(
                GET_SALES_TOOL,
                corporateApiClient.salesBaseUrl(),
                "/api/get-sales",
                queryParams);

        if (shouldRedactCustomerPii(redactCustomerPii)) {
            return redactSales(salesResponse);
        }
        return salesResponse;
    }

    String redactSales(String salesResponse) {
        return salesRecordRedactor.redactSales(salesResponse);
    }

    private boolean shouldRedactCustomerPii(boolean redactCustomerPii) {
        DemoUser demoUser = corporateApiAuthContext.get();

        if (SalesPiiRedactionPolicy.requiresRedactionForUser(demoUser) && !redactCustomerPii) {
            policyViolationTracker.recordSoft(
                    "getSales called without redactCustomerPii=true for a non-sales-admin user",
                    GET_SALES_TOOL);
        }

        return SalesPiiRedactionPolicy.shouldRedactCustomerPii(redactCustomerPii, demoUser);
    }
}
