package com.owasp.aiassistant.tools;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.stereotype.Component;

@Component
public class SalesRecordRedactor {

    private final ObjectMapper objectMapper;

    public SalesRecordRedactor(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public String redactSales(String salesJson) {
        if (salesJson == null || salesJson.isBlank()) {
            return salesJson;
        }

        try {
            JsonNode root = objectMapper.readTree(salesJson);
            if (!root.isArray()) {
                return salesJson;
            }

            ArrayNode sales = (ArrayNode) root;
            for (JsonNode saleNode : sales) {
                if (!saleNode.isObject()) {
                    continue;
                }
                ObjectNode sale = (ObjectNode) saleNode;
                if (sale.hasNonNull("customerName")) {
                    sale.put("customerName", redactValue(sale.get("customerName").asText()));
                }
                if (sale.hasNonNull("customerPhone")) {
                    sale.put("customerPhone", redactValue(sale.get("customerPhone").asText()));
                }
                sale.put("customerPiiRedacted", true);
            }

            return objectMapper.writeValueAsString(sales);
        } catch (Exception ex) {
            return salesJson;
        }
    }

    static String redactValue(String value) {
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
