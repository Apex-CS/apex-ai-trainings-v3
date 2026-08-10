package com.owasp.aiassistant.corporate.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Arrays;

public enum AppServerName {
    FINANCIAL_BACKEND("financial-backend"),
    IT_BACKEND("it-backend"),
    SALES_BACKEND("sales-backend"),
    MARKETING_BACKEND("marketing-backend");

    private final String apiValue;

    AppServerName(String apiValue) {
        this.apiValue = apiValue;
    }

    @JsonValue
    public String apiValue() {
        return apiValue;
    }

    @JsonCreator
    public static AppServerName fromValue(String value) {
        return Arrays.stream(values())
                .filter(server -> server.apiValue.equalsIgnoreCase(value)
                        || server.name().equalsIgnoreCase(value))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "Unknown app server. Use one of: financial-backend, it-backend, sales-backend, marketing-backend"));
    }
}
