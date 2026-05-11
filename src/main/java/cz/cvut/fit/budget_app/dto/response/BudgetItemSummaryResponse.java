package cz.cvut.fit.budget_app.dto.response;

import cz.cvut.fit.budget_app.entity.BudgetItem;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class BudgetItemSummaryResponse {

    /**
     * How to interpret {@link #remainingAmount} for this row: unspent expense budget vs income compared to plan.
     */
    public enum RemainingMetric {
        /** {@code plannedAmount − transactionAmount}: budget left on an expense line. */
        REMAINING_TO_SPEND,
        /** {@code transactionAmount − plannedAmount}: surplus / shortfall of realised income versus plan (same formula as balance on income budget items). */
        INCOME_VS_PLAN
    }

    private Long budgetItemId;
    private String budgetItemName;
    private BudgetItem.ItemType itemType;
    private BigDecimal plannedAmount;
    private BigDecimal transactionAmount;
    private RemainingMetric remainingMetric;
    private BigDecimal remainingAmount;
    /** {@code transactionAmount − plannedAmount}; positive means spending above plan or income above plan. */
    private BigDecimal varianceAmount;
}
