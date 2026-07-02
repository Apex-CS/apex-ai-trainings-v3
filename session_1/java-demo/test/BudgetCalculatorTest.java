/**
 * BudgetCalculatorTest — minimal test runner, no external dependencies.
 *
 * Compile and run from the java-demo folder:
 *   javac -d out src/BudgetCalculator.java test/BudgetCalculatorTest.java
 *   java -cp out BudgetCalculatorTest
 *
 * Training notes (Lab 5 — agentic workflow):
 *   - PASS  = the assertion holds.
 *   - FAIL  = the assertion was wrong — the implementation has a defect.
 *   - WARN  = the result is not an exception but is semantically suspicious.
 *   - INFO  = informational — no assertion, just observation.
 *
 * Lab 3 hallucination demo: before running this file, ask Copilot (no file in context):
 *   "What category does categorizeBudgetHealth return for a 10% savings rate?"
 *   "How many months does calculateAnnualProjection compound?"
 *   "What is the minimum emergency fund size in months?"
 * Record the answers. Then add BudgetCalculator.java to context and ask again.
 * The answers will change. That delta is the hallucination.
 */
public class BudgetCalculatorTest {

    private static int passed = 0;
    private static int failed = 0;

    public static void main(String[] args) {
        System.out.println("=== BudgetCalculator test run ===\n");

        // calculateMonthlyBudget
        testHappyPath();
        testExpensesExceedIncome();
        testNegativeFixedExpense();

        // calculateSavingsRate
        testZeroIncomeSavingsRate();
        testFullExpenseSavingsRate();

        // applyInflation
        testZeroInflation();
        testInflationTwelveMonths();

        // categorizeBudgetHealth — exposes private thresholds
        testCategorizeBoundaryAtRisk();
        testCategorizeBoundaryStable();
        testCategorizeExcellent();

        // calculateAnnualProjection — exposes off-by-one
        testAnnualProjectionIterationCount();

        // assessEmergencyFund
        testEmergencyFundExactThreshold();
        testEmergencyFundZeroCost();

        // parseCSVRow — exposes fragile parsing
        testParseCSVRowValidRow();
        testParseCSVRowHeaderCrash();

        System.out.println("\n--- " + passed + " passed, " + failed + " failed ---");
        if (failed > 0) {
            System.exit(1);
        }
    }

    // ------------------------------------------------------------------
    // calculateMonthlyBudget
    // ------------------------------------------------------------------

    static void testHappyPath() {
        double result = BudgetCalculator.calculateMonthlyBudget(5000, 2000, 800);
        assertEqual("Happy path: 5000 - 2000 - 800 = 2200", 2200.0, result, 0.01);
    }

    static void testExpensesExceedIncome() {
        double result = BudgetCalculator.calculateMonthlyBudget(2000, 2000, 500);
        assertEqual("Overspending: balance should be -500", -500.0, result, 0.01);
    }

    static void testNegativeFixedExpense() {
        // Defect 1: refund as negative fixed expense silently inflates the result.
        double result = BudgetCalculator.calculateMonthlyBudget(5000, -200, 800);
        System.out.println("WARN  testNegativeFixedExpense: result = " + result
                + " (negative expense accepted — 5000 - (-200) - 800 = 5400, intended?)");
    }

    // ------------------------------------------------------------------
    // calculateSavingsRate
    // ------------------------------------------------------------------

    static void testZeroIncomeSavingsRate() {
        // Defect 2: returns Infinity, not an exception.
        double result = BudgetCalculator.calculateSavingsRate(0, 500);
        if (Double.isInfinite(result) || Double.isNaN(result)) {
            System.out.println("WARN  testZeroIncomeSavingsRate: returned " + result
                    + " — no zero-guard");
        } else {
            fail("testZeroIncomeSavingsRate: expected Infinity or NaN, got " + result);
        }
    }

    static void testFullExpenseSavingsRate() {
        double result = BudgetCalculator.calculateSavingsRate(3000, 3000);
        assertEqual("Full spend: savings rate = 0%", 0.0, result, 0.01);
    }

    // ------------------------------------------------------------------
    // applyInflation
    // ------------------------------------------------------------------

    static void testZeroInflation() {
        double result = BudgetCalculator.applyInflation(1000.0, 0.0, 1000);
        assertEqual("Zero inflation 1000 months: no drift", 1000.0, result, 0.0001);
    }

    static void testInflationTwelveMonths() {
        double expected = 1000.0 * Math.pow(1.005, 12);
        double result   = BudgetCalculator.applyInflation(1000.0, 0.005, 12);
        assertEqual("0.5% monthly × 12 months", expected, result, 0.01);
    }

    // ------------------------------------------------------------------
    // categorizeBudgetHealth
    // Hallucination target: without the source, an AI will guess the thresholds.
    // These tests reveal the exact private constants used.
    // ------------------------------------------------------------------

    static void testCategorizeBoundaryAtRisk() {
        // A 10% savings rate falls in 5–15 → should be "STABLE", not "HEALTHY".
        // An AI without context often predicts "HEALTHY" or "GOOD" for 10%.
        String result = BudgetCalculator.categorizeBudgetHealth(10.0);
        assertEqual("10% savings rate → STABLE", "STABLE", result);
    }

    static void testCategorizeBoundaryStable() {
        // Exactly at the AT_RISK/STABLE boundary (5.0) → "STABLE"
        String result = BudgetCalculator.categorizeBudgetHealth(5.0);
        assertEqual("5.0% (AT_RISK threshold) → STABLE", "STABLE", result);
    }

    static void testCategorizeExcellent() {
        String result = BudgetCalculator.categorizeBudgetHealth(30.0);
        assertEqual("30% savings rate → EXCELLENT", "EXCELLENT", result);
    }

    // ------------------------------------------------------------------
    // calculateAnnualProjection
    // Defect 5: loop runs 13 times (i <= 12), not 12.
    // This test proves it by counting iterations via a known formula.
    // ------------------------------------------------------------------

    static void testAnnualProjectionIterationCount() {
        // With 0% return, total = monthlySavings × iterations (no compounding noise).
        // If the loop ran exactly 12 times: total = 12 × 500 = 6000.
        // Because the loop runs 13 times, total = 13 × 500 = 6500.
        double result = BudgetCalculator.calculateAnnualProjection(500.0, 0.0);
        if (Math.abs(result - 6500.0) < 0.01) {
            System.out.println("WARN  testAnnualProjectionIterationCount: result = " + result
                    + " — loop ran 13 times (off-by-one: i <= 12 instead of i < 12)");
        } else if (Math.abs(result - 6000.0) < 0.01) {
            fail("testAnnualProjectionIterationCount: got 6000, expected 6500 — off-by-one was fixed?");
        } else {
            fail("testAnnualProjectionIterationCount: unexpected result " + result);
        }
    }

    // ------------------------------------------------------------------
    // assessEmergencyFund
    // ------------------------------------------------------------------

    static void testEmergencyFundExactThreshold() {
        // 3 months of expenses = threshold (EMERGENCY_FUND_MONTHS = 3).
        // An AI without context may say the threshold is 6 months (industry rule of thumb).
        boolean result = BudgetCalculator.assessEmergencyFund(6000.0, 2000.0);
        assertEqual("3 months of expenses (6000 = 3 × 2000) → true", true, result);
    }

    static void testEmergencyFundZeroCost() {
        // Edge case: zero monthly expenses always satisfies any savings threshold.
        boolean result = BudgetCalculator.assessEmergencyFund(0.0, 0.0);
        System.out.println("WARN  testEmergencyFundZeroCost: result = " + result
                + " (0 savings covers 0 expenses — semantically ambiguous)");
    }

    // ------------------------------------------------------------------
    // parseCSVRow
    // ------------------------------------------------------------------

    static void testParseCSVRowValidRow() {
        // January row: "January,5000.00,2000.00,800.00,Normal month"
        double[] result = BudgetCalculator.parseCSVRow("January,5000.00,2000.00,800.00,Normal month");
        assertEqual("parseCSVRow income", 5000.0, result[0], 0.01);
        assertEqual("parseCSVRow fixedExpenses", 2000.0, result[1], 0.01);
        assertEqual("parseCSVRow variableExpenses", 800.0, result[2], 0.01);
    }

    static void testParseCSVRowHeaderCrash() {
        // Defect 6b: the header row contains "income" (non-numeric) → NumberFormatException.
        // This test documents the crash rather than hiding it.
        try {
            BudgetCalculator.parseCSVRow("month,income,fixed_expenses,variable_expenses,notes");
            fail("testParseCSVRowHeaderCrash: expected NumberFormatException — none thrown");
        } catch (NumberFormatException e) {
            System.out.println("WARN  testParseCSVRowHeaderCrash: throws NumberFormatException"
                    + " on header row — no guard in parseCSVRow");
        }
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    static void assertEqual(String label, double expected, double actual, double tolerance) {
        if (Math.abs(expected - actual) <= tolerance) {
            System.out.println("PASS  " + label);
            passed++;
        } else {
            System.out.println("FAIL  " + label
                    + " | expected=" + expected + " actual=" + actual);
            failed++;
        }
    }

    static void assertEqual(String label, String expected, String actual) {
        if (expected.equals(actual)) {
            System.out.println("PASS  " + label);
            passed++;
        } else {
            System.out.println("FAIL  " + label
                    + " | expected=" + expected + " actual=" + actual);
            failed++;
        }
    }

    static void assertEqual(String label, boolean expected, boolean actual) {
        if (expected == actual) {
            System.out.println("PASS  " + label);
            passed++;
        } else {
            System.out.println("FAIL  " + label
                    + " | expected=" + expected + " actual=" + actual);
            failed++;
        }
    }

    static void fail(String message) {
        System.out.println("FAIL  " + message);
        failed++;
    }
}
