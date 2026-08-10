package com.owasp.financial.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record UpdateBudgetRequest(
        @NotBlank
        @Size(max = 20)
        @Pattern(regexp = "(?i)IT|FINANCE|SALES|MARKETING", message = "area must be IT, FINANCE, SALES, or MARKETING")
        String area,

        @NotNull
        @Min(1)
        @Max(4)
        Integer fiscalQuarter,

        @NotNull
        @Min(2020)
        @Max(2100)
        Integer fiscalYear,

        @NotNull
        @DecimalMin(value = "0.01", message = "budget must be greater than zero")
        BigDecimal budget) {
}
