package cz.cvut.fit.budget_app.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class RealizePlannedTransactionRequest {

    @NotNull
    private LocalDate date;

    @NotNull
    @Positive
    private BigDecimal amount;

    @Size(max = 2000)
    private String description;
}
