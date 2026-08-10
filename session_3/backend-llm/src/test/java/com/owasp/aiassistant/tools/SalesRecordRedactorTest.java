package com.owasp.aiassistant.tools;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SalesRecordRedactorTest {

    private final SalesRecordRedactor redactor = new SalesRecordRedactor(new com.fasterxml.jackson.databind.ObjectMapper());

    @Test
    void redactsCustomerNameAndPhoneInSalesArray() throws Exception {
        String input = """
                [
                  {
                    "id": 1,
                    "productCode": "CLASSIC_YELLOW",
                    "productName": "Classic Yellow Rubber Duck",
                    "purchaseDate": "2024-04-05T11:14:00",
                    "salePrice": 13.01,
                    "customerName": "Avery Hall",
                    "customerPhone": "+1-555-303-992863",
                    "customerPiiRedacted": false
                  }
                ]
                """;

        String redacted = redactor.redactSales(input);

        assertTrue(redacted.contains("\"customerName\":\"***** ****\""));
        assertTrue(redacted.contains("\"customerPhone\":\"+*-***-***-******\""));
        assertTrue(redacted.contains("\"customerPiiRedacted\":true"));
    }

    @Test
    void leavesNonArrayResponsesUnchanged() {
        String error = "Corporate API error (HTTP 403): forbidden";
        assertEquals(error, redactor.redactSales(error));
    }

    @Test
    void redactValuePreservesSeparators() {
        assertEquals("***** ****", SalesRecordRedactor.redactValue("Avery Hall"));
        assertEquals("+*-***-***-******", SalesRecordRedactor.redactValue("+1-555-303-992863"));
    }
}
