package com.owasp.financial.service;

import com.owasp.financial.domain.Budget;
import com.owasp.financial.dto.BudgetResponse;
import com.owasp.financial.dto.UpdateBudgetRequest;
import com.owasp.financial.repository.BudgetRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class BudgetService {

    private final BudgetRepository budgetRepository;

    public BudgetService(BudgetRepository budgetRepository) {
        this.budgetRepository = budgetRepository;
    }

    @Transactional(readOnly = true)
    public List<BudgetResponse> getBudgetByArea(String area, Integer fiscalYear) {
        String normalizedArea = area.trim().toUpperCase();
        List<Budget> budgets = fiscalYear == null
                ? budgetRepository.findByAreaIgnoreCaseOrderByFiscalYearAscFiscalQuarterAsc(normalizedArea)
                : budgetRepository.findByAreaIgnoreCaseAndFiscalYearOrderByFiscalQuarterAsc(normalizedArea, fiscalYear);

        if (budgets.isEmpty()) {
            throw new BudgetNotFoundException("No budget records found for area: " + normalizedArea);
        }

        return budgets.stream().map(this::toResponse).toList();
    }

    @Transactional
    public BudgetResponse upsertBudget(UpdateBudgetRequest request, String username) {
        String normalizedArea = request.area().trim().toUpperCase();
        Budget budget = budgetRepository
                .findByAreaIgnoreCaseAndFiscalYearAndFiscalQuarter(
                        normalizedArea, request.fiscalYear(), request.fiscalQuarter())
                .orElseGet(Budget::new);

        budget.setArea(normalizedArea);
        budget.setFiscalQuarter(request.fiscalQuarter());
        budget.setFiscalYear(request.fiscalYear());
        budget.setBudget(request.budget());
        budget.setUserModified(username);
        budget.setUpdatedAt(LocalDateTime.now());

        return toResponse(budgetRepository.save(budget));
    }

    private BudgetResponse toResponse(Budget budget) {
        return new BudgetResponse(
                budget.getId(),
                budget.getArea(),
                budget.getFiscalQuarter(),
                budget.getFiscalYear(),
                budget.getBudget(),
                budget.getUserModified(),
                budget.getUpdatedAt());
    }
}
