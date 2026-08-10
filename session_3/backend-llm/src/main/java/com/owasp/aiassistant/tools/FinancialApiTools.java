package com.owasp.aiassistant.tools;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.owasp.aiassistant.corporate.client.CorporateApiClient;
import com.owasp.aiassistant.corporate.enums.BudgetArea;
import com.owasp.aiassistant.corporate.enums.FiscalQuarter;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class FinancialApiTools {

    private static final String GET_BUDGET_TOOL = "getBudgetByArea";
    private static final String UPDATE_BUDGET_TOOL = "updateBudgetByArea";

    private final CorporateApiClient corporateApiClient;
    private final ObjectMapper objectMapper;

    public FinancialApiTools(CorporateApiClient corporateApiClient, ObjectMapper objectMapper) {
        this.corporateApiClient = corporateApiClient;
        this.objectMapper = objectMapper;
    }

    @Tool(description = """
            Retrieve quarterly budget records from the Financial API for a business area.
            Requires the chat user to have financial-admin or financial-user role.
            Use BudgetArea enum values: IT, FINANCE, SALES, MARKETING.
            """)
    public String getBudgetByArea(
            @ToolParam(description = "Business area: IT, FINANCE, SALES, or MARKETING")
            BudgetArea area,
            @ToolParam(description = "Optional fiscal year filter, e.g. 2026", required = false)
            Integer fiscalYear) {
        Map<String, String> queryParams = new LinkedHashMap<>();
        queryParams.put("area", area.apiValue());
        if (fiscalYear != null) {
            queryParams.put("fiscalYear", String.valueOf(fiscalYear));
        }

        return corporateApiClient.get(
                GET_BUDGET_TOOL,
                corporateApiClient.financialBaseUrl(),
                "/api/get-budget-by-area",
                queryParams);
    }

    @Tool(description = """
            Upsert a quarterly budget record in the Financial API for a business area.
            Requires the chat user to have financial-admin role.
            Use BudgetArea enum values: IT, FINANCE, SALES, MARKETING.
            Use FiscalQuarter enum values: Q1, Q2, Q3, Q4.
            """)
    public String updateBudgetByArea(
            @ToolParam(description = "Business area: IT, FINANCE, SALES, or MARKETING")
            BudgetArea area,
            @ToolParam(description = "Fiscal quarter: Q1, Q2, Q3, or Q4")
            FiscalQuarter fiscalQuarter,
            @ToolParam(description = "Fiscal year, e.g. 2026")
            Integer fiscalYear,
            @ToolParam(description = "Budget amount in dollars, e.g. 375000.00")
            BigDecimal budget) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("area", area.apiValue());
        body.put("fiscalQuarter", fiscalQuarter.apiValue());
        body.put("fiscalYear", fiscalYear);
        body.put("budget", budget);

        try {
            return corporateApiClient.put(
                    UPDATE_BUDGET_TOOL,
                    corporateApiClient.financialBaseUrl(),
                    "/api/update-budget-by-area",
                    objectMapper.writeValueAsString(body));
        } catch (JsonProcessingException e) {
            return "Failed to build budget update request: " + e.getMessage();
        }
    }
}
