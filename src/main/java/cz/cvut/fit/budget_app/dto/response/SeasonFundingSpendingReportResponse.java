package cz.cvut.fit.budget_app.dto.response;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class SeasonFundingSpendingReportResponse {
    private Long seasonId;
    private int sourceCount;
    private int limitExceededCount;
    private BigDecimal totalAllocatedAmount;
    private BigDecimal totalActualIncome;
    private BigDecimal totalActualExpense;
    private BigDecimal totalPlannedIncome;
    private BigDecimal totalPlannedExpense;
    private BigDecimal totalRemainingForExpense;
    private BigDecimal totalActualSpending;
    private BigDecimal totalRemainingAmount;
    private List<FundingSourceResponse> sources;
}
