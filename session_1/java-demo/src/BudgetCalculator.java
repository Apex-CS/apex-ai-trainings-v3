
public class BudgetCalculator {

    private static final double CRITICAL_THRESHOLD  = -10.0;
    private static final double AT_RISK_THRESHOLD   =   5.0;
    private static final double STABLE_THRESHOLD    =  15.0;
    private static final double HEALTHY_THRESHOLD   =  25.0;
    private static final int    EMERGENCY_FUND_MONTHS = 3;

    // -----------------------------------------------------------------------
    // Core calculations
    // -----------------------------------------------------------------------

    /**
     * Returns the remaining budget after fixed and variable expenses.
     *
     * @param income            monthly gross income
     * @param fixedExpenses     rent, subscriptions, loan payments
     * @param variableExpenses  groceries, transport, discretionary spending
     * @return remaining amount (may be negative if expenses exceed income)
     */
    public static double calculateMonthlyBudget(double income,
                                                double fixedExpenses,
                                                double variableExpenses) {
        // Defect 1: no validation — negative expense values are accepted silently.
        return income - fixedExpenses - variableExpenses;
    }

    /**
     * Returns the percentage of income that was saved this month.
     *
     * @param income         monthly gross income
     * @param totalExpenses  sum of all expenses for the month
     * @return savings as a percentage of income (0–100 scale)
     */
    public static double calculateSavingsRate(double income, double totalExpenses) {
        // Defect 2: if income is 0, this returns Infinity — no zero-guard.
        return ((income - totalExpenses) / income) * 100;
    }

    /**
     * Compounds an amount by a monthly inflation rate over a number of months.
     *
     * @param amount       starting amount
     * @param monthlyRate  monthly rate as a decimal (e.g. 0.005 for 0.5%)
     * @param months       number of months to apply
     * @return inflated amount
     */
    public static double applyInflation(double amount, double monthlyRate, int months) {
        // Defect 3: floating-point drift accumulates; imperceptible at 12 months,
        // measurable at hundreds of iterations.
        for (int i = 0; i < months; i++) {
            amount = amount * (1 + monthlyRate);
        }
        return amount;
    }

    // -----------------------------------------------------------------------
    // Budget health classification
    // -----------------------------------------------------------------------

    /**
     * Maps a savings rate (%) to a health category string.
     *
     *
     * @param savingsRate  output of calculateSavingsRate()
     * @return one of: "CRITICAL", "AT_RISK", "STABLE", "HEALTHY", "EXCELLENT"
     */
    public static String categorizeBudgetHealth(double savingsRate) {
        if (savingsRate < CRITICAL_THRESHOLD) return "CRITICAL";
        if (savingsRate < AT_RISK_THRESHOLD)  return "AT_RISK";
        if (savingsRate < STABLE_THRESHOLD)   return "STABLE";
        if (savingsRate < HEALTHY_THRESHOLD)  return "HEALTHY";
        return "EXCELLENT";
    }

    // -----------------------------------------------------------------------
    // Projection
    // -----------------------------------------------------------------------

    /**
     * Projects the future value of monthly savings compounded at an annual return rate.
     *
     * Intended to model 12 months of contributions each earning a return.
     *
     * @param monthlySavings   fixed amount saved each month
     * @param annualReturnRate annual rate as a decimal (e.g. 0.06 for 6%)
     * @return projected total after all contributions and compounding
     */
    public static double calculateAnnualProjection(double monthlySavings,
                                                   double annualReturnRate) {
        double total = 0;
        double monthlyRate = annualReturnRate / 12.0;
        for (int i = 0; i <= 12; i++) {
            total = (total + monthlySavings) * (1 + monthlyRate);
        }
        return total;
    }

    // -----------------------------------------------------------------------
    // Emergency fund assessment
    // -----------------------------------------------------------------------

    /**
     * Returns true if the current savings cover the minimum emergency fund.
     *
     * @param savings          total liquid savings
     * @param monthlyExpenses  average total monthly expenses
     * @return true if savings meet or exceed the minimum threshold
     */
    public static boolean assessEmergencyFund(double savings, double monthlyExpenses) {
        // Edge case: if monthlyExpenses is 0, this always returns true.
        // Any savings amount satisfies a 0-cost threshold.
        return savings >= (monthlyExpenses * EMERGENCY_FUND_MONTHS);
    }

    // -----------------------------------------------------------------------
    // CSV parsing
    // -----------------------------------------------------------------------

    /**
     * Parses one data row from sample_budget.csv and returns [income, fixed, variable].
     *
     * Expected format: month,income,fixed_expenses,variable_expenses,notes
     *
     * @param csvLine  a single raw line from the CSV file
     * @return double array: [income, fixedExpenses, variableExpenses]
     */
    public static double[] parseCSVRow(String csvLine) {
        String[] parts = csvLine.split(",");
        double income           = Double.parseDouble(parts[1].trim());
        double fixedExpenses    = Double.parseDouble(parts[2].trim());
        double variableExpenses = Double.parseDouble(parts[3].trim());
        return new double[]{income, fixedExpenses, variableExpenses};
    }
}
