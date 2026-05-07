package cz.cvut.fit.budget_app.dto.response;

import cz.cvut.fit.budget_app.entity.Transaction;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class TransactionResponse {
    private Long id;
    private LocalDate date;
    private BigDecimal amount;
    private Transaction.TransactionType type;
    private Transaction.Direction direction;
    private String description;
    private Long budgetItemId;
    private Long seasonId;

    public static TransactionResponse from(Transaction t) {
        TransactionResponse r = new TransactionResponse();
        r.id = t.getId();
        r.date = t.getDate();
        r.amount = t.getAmount();
        r.type = t.getType();
        r.direction = t.getDirection();
        r.description = t.getDescription();
        r.budgetItemId = t.getBudgetItem().getId();
        r.seasonId = t.getSeason().getId();
        return r;
    }
}
