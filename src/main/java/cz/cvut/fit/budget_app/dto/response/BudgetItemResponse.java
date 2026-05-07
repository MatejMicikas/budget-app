package cz.cvut.fit.budget_app.dto.response;

import cz.cvut.fit.budget_app.entity.BudgetItem;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class BudgetItemResponse {
    private Long id;
    private String name;
    private BudgetItem.ItemType type;
    private BigDecimal plannedAmount;
    private BigDecimal actualAmount;
    private BigDecimal balance;
    private Long seasonId;
    private Long fundingSourceId;

    public static BudgetItemResponse from(BudgetItem item, BigDecimal actualAmount) {
        BudgetItemResponse r = new BudgetItemResponse();
        r.id = item.getId();
        r.name = item.getName();
        r.type = item.getType();
        r.plannedAmount = item.getPlannedAmount();
        r.actualAmount = actualAmount;
        r.balance = calculateBalance(item, actualAmount);
        r.seasonId = item.getSeason().getId();
        r.fundingSourceId = item.getFundingSource() != null ? item.getFundingSource().getId() : null;
        return r;
    }

    private static BigDecimal calculateBalance(BudgetItem item, BigDecimal actualAmount) {
        if (item.getType() == BudgetItem.ItemType.EXPENSE) {
            // Remaining budget: positive means there is still money left to spend.
            return item.getPlannedAmount().subtract(actualAmount);
        }
        // Income variance: positive means collected income is above plan.
        return actualAmount.subtract(item.getPlannedAmount());
    }
}
