package com.owasp.sales.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record SaleResponse(
        Integer id,
        String productCode,
        String productName,
        LocalDateTime purchaseDate,
        BigDecimal salePrice,
        String customerName,
        String customerPhone,
        boolean customerPiiRedacted) {
}
