package cz.cvut.fit.budget_app.dto.response;

import cz.cvut.fit.budget_app.entity.BudgetItem;
import cz.cvut.fit.budget_app.entity.Transaction;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class BudgetSummaryResponse {
    private Long seasonId;
    private BudgetItem.ItemType itemTypeFilter;
    private Transaction.TransactionType transactionTypeUsed;
    private int itemCount;
    private BigDecimal totalPlannedAmount;
    private BigDecimal totalTransactionAmount;
    /** Sum of expense lines only ({@link BudgetItemSummaryResponse.RemainingMetric#REMAINING_TO_SPEND}). */
    private BigDecimal totalRemainingAmount;
    /** Sum over all lines: transaction amount minus planned ({@link BudgetItemSummaryResponse#getVarianceAmount}). */
    private BigDecimal totalVarianceAmount;
    private List<BudgetItemSummaryResponse> items;
}
