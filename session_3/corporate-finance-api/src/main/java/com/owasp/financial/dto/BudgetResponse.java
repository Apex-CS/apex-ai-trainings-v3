package com.owasp.financial.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record BudgetResponse(
        Integer id,
        String area,
        Integer fiscalQuarter,
        Integer fiscalYear,
        BigDecimal budget,
        String userModified,
        LocalDateTime updatedAt) {
}
