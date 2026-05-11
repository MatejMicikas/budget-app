package cz.cvut.fit.budget_app.dto.response;

import cz.cvut.fit.budget_app.entity.FundingSource;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class FundingSourceResponse {
    private Long id;
    private String name;
    private FundingSource.FundingType type;
    private BigDecimal allocatedAmount;
    private BigDecimal actualIncome;
    private BigDecimal actualExpense;
    private BigDecimal plannedIncome;
    private BigDecimal plannedExpense;
    private BigDecimal remainingForExpense;
    private BigDecimal actualSpending;
    private boolean limitExceeded;
    private Long seasonId;

    public static FundingSourceResponse from(FundingSource fs,
                                             BigDecimal actualIncome,
                                             BigDecimal actualExpense,
                                             BigDecimal plannedIncome,
                                             BigDecimal plannedExpense) {
        FundingSourceResponse r = new FundingSourceResponse();
        r.id = fs.getId();
        r.name = fs.getName();
        r.type = fs.getType();
        r.allocatedAmount = fs.getAllocatedAmount();
        r.actualIncome = actualIncome;
        r.actualExpense = actualExpense;
        r.plannedIncome = plannedIncome;
        r.plannedExpense = plannedExpense;
        r.remainingForExpense = fs.getAllocatedAmount() != null
                ? fs.getAllocatedAmount().subtract(actualExpense)
                : null;
        // Backward-compatible alias for existing clients.
        r.actualSpending = actualExpense;
        r.limitExceeded = fs.getAllocatedAmount() != null
                && actualExpense.compareTo(fs.getAllocatedAmount()) > 0;
        r.seasonId = fs.getSeason().getId();
        return r;
    }
}
