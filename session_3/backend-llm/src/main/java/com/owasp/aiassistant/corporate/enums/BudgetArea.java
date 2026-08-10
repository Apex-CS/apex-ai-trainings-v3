package com.owasp.aiassistant.corporate.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Arrays;

public enum BudgetArea {
    IT("IT"),
    FINANCE("FINANCE"),
    SALES("SALES"),
    MARKETING("MARKETING");

    private final String apiValue;

    BudgetArea(String apiValue) {
        this.apiValue = apiValue;
    }

    @JsonValue
    public String apiValue() {
        return apiValue;
    }

    @JsonCreator
    public static BudgetArea fromValue(String value) {
        return Arrays.stream(values())
                .filter(area -> area.apiValue.equalsIgnoreCase(value)
                        || area.name().equalsIgnoreCase(value))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "Unknown budget area. Use one of: IT, FINANCE, SALES, MARKETING"));
    }
}
