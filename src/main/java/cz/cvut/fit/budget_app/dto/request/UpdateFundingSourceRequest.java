package cz.cvut.fit.budget_app.dto.request;

import cz.cvut.fit.budget_app.entity.FundingSource;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class UpdateFundingSourceRequest {

    @NotBlank
    private String name;

    @NotNull
    private FundingSource.FundingType type;

    @Positive
    private BigDecimal allocatedAmount;
}
