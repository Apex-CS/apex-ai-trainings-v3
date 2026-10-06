package com.workshop.concierge.guardrail;

import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * PII &amp; Sensitive Filter — redacts personal addresses and payment details that may appear in
 * uploaded receipt text before it is handed to any downstream agent or stored in memory.
 */
@Component
public class PiiSensitiveFilter {

    private static final Logger log = LoggerFactory.getLogger(PiiSensitiveFilter.class);

    // 13-19 digit card numbers, optionally grouped by spaces/dashes.
    private static final Pattern CARD_NUMBER = Pattern.compile("\\b(?:\\d[ -]?){13,19}\\b");
    // CVV-like 3-4 digit codes preceded by a label.
    private static final Pattern CVV = Pattern.compile("(?i)\\b(cvv|cvc)\\s*[:#]?\\s*\\d{3,4}\\b");
    // Simple US-style street address: number + street name + suffix.
    private static final Pattern STREET_ADDRESS = Pattern.compile(
            "(?i)\\b\\d{1,5}\\s+([A-Za-z0-9.'-]+\\s){1,4}(street|st|avenue|ave|road|rd|boulevard|blvd|lane|ln|drive|dr|court|ct|way|place|pl)\\b");
    // Email addresses.
    private static final Pattern EMAIL = Pattern.compile("(?i)\\b[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,}\\b");
    // Phone numbers.
    private static final Pattern PHONE = Pattern.compile("\\b(?:\\+?\\d{1,2}[ -]?)?(?:\\(\\d{3}\\)|\\d{3})[ -]?\\d{3}[ -]?\\d{4}\\b");

    public String redact(String text) {
        if (text == null || text.isBlank()) {
            return text;
        }
        String redacted = text;
        redacted = CARD_NUMBER.matcher(redacted).replaceAll("[REDACTED-CARD-NUMBER]");
        redacted = CVV.matcher(redacted).replaceAll("[REDACTED-CVV]");
        redacted = STREET_ADDRESS.matcher(redacted).replaceAll("[REDACTED-ADDRESS]");
        redacted = EMAIL.matcher(redacted).replaceAll("[REDACTED-EMAIL]");
        redacted = PHONE.matcher(redacted).replaceAll("[REDACTED-PHONE]");

        if (!redacted.equals(text)) {
            log.info("[PiiSensitiveFilter] redacted sensitive content from input text");
        }
        return redacted;
    }
}
