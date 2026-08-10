package com.owasp.aiassistant.corporate.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Arrays;

public enum FiscalQuarter {
    Q1(1),
    Q2(2),
    Q3(3),
    Q4(4);

    private final int apiValue;

    FiscalQuarter(int apiValue) {
        this.apiValue = apiValue;
    }

    @JsonValue
    public int apiValue() {
        return apiValue;
    }

    @JsonCreator
    public static FiscalQuarter fromValue(Object value) {
        if (value instanceof Number number) {
            return fromInt(number.intValue());
        }
        return Arrays.stream(values())
                .filter(quarter -> quarter.name().equalsIgnoreCase(String.valueOf(value)))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Fiscal quarter must be Q1, Q2, Q3, or Q4"));
    }

    public static FiscalQuarter fromInt(int quarter) {
        return Arrays.stream(values())
                .filter(item -> item.apiValue == quarter)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Fiscal quarter must be between 1 and 4"));
    }
}
