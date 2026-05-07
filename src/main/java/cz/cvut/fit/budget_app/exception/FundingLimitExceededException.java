package cz.cvut.fit.budget_app.exception;

import java.math.BigDecimal;

public class FundingLimitExceededException extends RuntimeException {
    public FundingLimitExceededException(Long fundingSourceId, BigDecimal limit, BigDecimal actual) {
        super("FundingSource " + fundingSourceId + " limit " + limit + " exceeded by actual spending " + actual);
    }
}
