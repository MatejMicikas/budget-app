package cz.cvut.fit.budget_app.dto.request;

import cz.cvut.fit.budget_app.entity.BudgetItem;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class UpdateBudgetItemRequest {

    @NotBlank
    @Size(max = 100)
    private String name;

    @NotNull
    private BudgetItem.ItemType type;

    @NotNull
    @Positive
    private BigDecimal plannedAmount;

    private Long fundingSourceId;
}
