package com.owasp.financial.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "budget")
@Getter
@Setter
@NoArgsConstructor
public class Budget {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false, length = 20)
    private String area;

    @Column(name = "fiscal_quarter", nullable = false)
    private Integer fiscalQuarter;

    @Column(name = "fiscal_year", nullable = false)
    private Integer fiscalYear;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal budget;

    @Column(name = "user_modified", length = 30)
    private String userModified;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
