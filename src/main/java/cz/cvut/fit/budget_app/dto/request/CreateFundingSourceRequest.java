package cz.cvut.fit.budget_app.dto.request;

import cz.cvut.fit.budget_app.entity.FundingSource;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class CreateFundingSourceRequest {

    @NotBlank
    @Size(max = 100)
    private String name;

    @NotNull
    private FundingSource.FundingType type;

    /**
     * Optional maximum amount for this source in the season. {@code null} means no limit.
     * When set, must be strictly positive ({@code @Positive} allows {@code null}).
     */
    @Positive
    private BigDecimal allocatedAmount;

    @NotNull
    private Long seasonId;
}
