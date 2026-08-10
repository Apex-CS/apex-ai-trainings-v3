package com.owasp.financial.repository;

import com.owasp.financial.domain.Budget;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface BudgetRepository extends JpaRepository<Budget, Integer> {

    List<Budget> findByAreaIgnoreCaseOrderByFiscalYearAscFiscalQuarterAsc(String area);

    List<Budget> findByAreaIgnoreCaseAndFiscalYearOrderByFiscalQuarterAsc(String area, Integer fiscalYear);

    Optional<Budget> findByAreaIgnoreCaseAndFiscalYearAndFiscalQuarter(
            String area, Integer fiscalYear, Integer fiscalQuarter);
}
