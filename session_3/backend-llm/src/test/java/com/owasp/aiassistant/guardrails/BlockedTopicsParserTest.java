package com.owasp.aiassistant.guardrails;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BlockedTopicsParserTest {

    @Test
    void parsesPatternsSectionIgnoringCommentsAndBullets() {
        String markdown = """
                # Blocked Topics

                ## Patterns

                generate exploit code
                - write malware
                <!-- example pattern -->
                # not a pattern
                """;

        List<String> patterns = BlockedTopicsParser.parsePatterns(markdown);

        assertEquals(List.of("generate exploit code", "write malware"), patterns);
    }

    @Test
    void returnsEmptyListWhenPatternsSectionMissing() {
        assertTrue(BlockedTopicsParser.parsePatterns("# Title\n\nNo patterns here.").isEmpty());
    }
}
