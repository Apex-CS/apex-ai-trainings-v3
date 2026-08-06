package com.owasp.aiassistant.guardrails;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

final class BlockedTopicsParser {

    private static final String PATTERNS_HEADER = "## patterns";

    private BlockedTopicsParser() {
    }

    static List<String> parsePatterns(String markdown) {
        if (markdown == null || markdown.isBlank()) {
            return List.of();
        }

        String[] lines = markdown.replace("\r\n", "\n").split("\n");
        boolean inPatternsSection = false;
        List<String> patterns = new ArrayList<>();

        for (String rawLine : lines) {
            String line = rawLine.trim();
            if (line.isEmpty()) {
                continue;
            }
            if (isPatternsHeader(line)) {
                inPatternsSection = true;
                continue;
            }
            if (inPatternsSection && line.startsWith("## ")) {
                break;
            }
            if (inPatternsSection) {
                if (line.startsWith("<!--") || line.startsWith("#")) {
                    continue;
                }
                patterns.add(stripListMarker(line));
            }
        }

        return List.copyOf(patterns);
    }

    private static boolean isPatternsHeader(String line) {
        return line.toLowerCase(Locale.ROOT).startsWith(PATTERNS_HEADER);
    }

    private static String stripListMarker(String line) {
        if (line.startsWith("- ")) {
            return line.substring(2).trim();
        }
        if (line.startsWith("* ")) {
            return line.substring(2).trim();
        }
        return line;
    }
}
