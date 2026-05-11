package cz.cvut.fit.budget_app.dto.request;

import cz.cvut.fit.budget_app.entity.FundingSource;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class UpdateFundingSourceRequest {

    @NotBlank
    @Size(max = 100)
    private String name;

    @NotNull
    private FundingSource.FundingType type;

    /**
     * Optional maximum amount. {@code null} clears the limit (no cap).
     * When set, must be strictly positive.
     */
    @Positive
    private BigDecimal allocatedAmount;
}
