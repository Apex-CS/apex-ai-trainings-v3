package com.owasp.sales.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PiiRedactorTest {

    @Test
    void redactsLettersAndDigitsButPreservesSeparators() {
        assertEquals("****** ****", PiiRedactor.redact("Morgan Hall"));
        assertEquals("+*-***-***-******", PiiRedactor.redact("+1-555-189-791214"));
    }

    @Test
    void leavesBlankValuesUnchanged() {
        assertEquals("", PiiRedactor.redact(""));
        assertEquals(null, PiiRedactor.redact(null));
    }
}
