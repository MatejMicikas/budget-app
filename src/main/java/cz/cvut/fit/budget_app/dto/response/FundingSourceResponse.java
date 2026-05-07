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
    private BigDecimal actualSpending;
    private boolean limitExceeded;
    private Long seasonId;

    public static FundingSourceResponse from(FundingSource fs, BigDecimal actualSpending) {
        FundingSourceResponse r = new FundingSourceResponse();
        r.id = fs.getId();
        r.name = fs.getName();
        r.type = fs.getType();
        r.allocatedAmount = fs.getAllocatedAmount();
        r.actualSpending = actualSpending;
        r.limitExceeded = fs.getAllocatedAmount() != null
                && actualSpending.compareTo(fs.getAllocatedAmount()) > 0;
        r.seasonId = fs.getSeason().getId();
        return r;
    }
}
