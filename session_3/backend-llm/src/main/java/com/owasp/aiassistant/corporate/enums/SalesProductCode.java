package com.owasp.aiassistant.corporate.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Arrays;

public enum SalesProductCode {
    CLASSIC_YELLOW("CLASSIC_YELLOW"),
    GLOW_DUCKLING("GLOW_DUCKLING"),
    CORP_EVENT_DUCK("CORP_EVENT_DUCK"),
    GLOBAL_DISTRO_KIT("GLOBAL_DISTRO_KIT"),
    RETAIL_PARTNER_PACK("RETAIL_PARTNER_PACK"),
    POOL_PARTY_BUNDLE("POOL_PARTY_BUNDLE"),
    COLLECTOR_GOLDEN("COLLECTOR_GOLDEN"),
    CUSTOMER_SUCCESS_KIT("CUSTOMER_SUCCESS_KIT");

    private final String apiValue;

    SalesProductCode(String apiValue) {
        this.apiValue = apiValue;
    }

    @JsonValue
    public String apiValue() {
        return apiValue;
    }

    @JsonCreator
    public static SalesProductCode fromValue(String value) {
        return Arrays.stream(values())
                .filter(code -> code.apiValue.equalsIgnoreCase(value)
                        || code.name().equalsIgnoreCase(value))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "Unknown product code. Use one of: CLASSIC_YELLOW, GLOW_DUCKLING, CORP_EVENT_DUCK, "
                                + "GLOBAL_DISTRO_KIT, RETAIL_PARTNER_PACK, POOL_PARTY_BUNDLE, COLLECTOR_GOLDEN, "
                                + "CUSTOMER_SUCCESS_KIT"));
    }
}
