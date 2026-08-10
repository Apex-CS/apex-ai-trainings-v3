package com.owasp.sales.util;

public final class PiiRedactor {

    private PiiRedactor() {
    }

    public static String redact(String value) {
        if (value == null || value.isBlank()) {
            return value;
        }

        StringBuilder redacted = new StringBuilder(value.length());
        for (char character : value.toCharArray()) {
            if (Character.isLetterOrDigit(character)) {
                redacted.append('*');
            } else {
                redacted.append(character);
            }
        }
        return redacted.toString();
    }
}
