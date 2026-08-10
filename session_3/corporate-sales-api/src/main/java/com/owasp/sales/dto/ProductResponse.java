package com.owasp.sales.dto;

import java.math.BigDecimal;

public record ProductResponse(
        Integer id,
        String productCode,
        String name,
        String description,
        BigDecimal listPrice) {
}
