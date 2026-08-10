package com.owasp.aiassistant.corporate.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Arrays;

public enum DemoUser {
    FULANO_SMITH("fulano.smith"),
    SUTANO_DOE("sutano.doe"),
    MENGANA_DAVIDSON("mengana.davidson"),
    BART_PEREZ("bart.perez");

    private final String username;
    private final String displayName;

    DemoUser(String username) {
        this(username, formatDisplayName(username));
    }

    DemoUser(String username, String displayName) {
        this.username = username;
        this.displayName = displayName;
    }

    @JsonValue
    public String username() {
        return username;
    }

    public String displayName() {
        return displayName;
    }

    private static String formatDisplayName(String username) {
        String[] parts = username.split("\\.");
        StringBuilder builder = new StringBuilder();
        for (int index = 0; index < parts.length; index++) {
            if (index > 0) {
                builder.append(' ');
            }
            String part = parts[index];
            if (!part.isEmpty()) {
                builder.append(Character.toUpperCase(part.charAt(0)));
                if (part.length() > 1) {
                    builder.append(part.substring(1));
                }
            }
        }
        return builder.toString();
    }

    @JsonCreator
    public static DemoUser fromValue(String value) {
        return Arrays.stream(values())
                .filter(user -> user.username.equalsIgnoreCase(value)
                        || user.name().equalsIgnoreCase(value))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "Unknown demo user. Use one of: fulano.smith, sutano.doe, mengana.davidson, bart.perez"));
    }
}
