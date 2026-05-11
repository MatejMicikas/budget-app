package cz.cvut.fit.budget_app.dto.request;

import cz.cvut.fit.budget_app.entity.Transaction;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class CreateTransactionRequest {

    @NotNull
    private LocalDate date;

    @NotNull
    @Positive
    private BigDecimal amount;

    @NotNull
    private Transaction.TransactionType type;

    @NotNull
    private Transaction.Direction direction;

    @Size(max = 2000)
    private String description;

    @NotNull
    private Long seasonId;

    @NotNull
    private Long budgetItemId;
}
