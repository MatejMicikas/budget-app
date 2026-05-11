package cz.cvut.fit.budget_app.dto.response;

import cz.cvut.fit.budget_app.entity.Transaction;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

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
    private Long plannedTransactionId;
    private Transaction.ApprovalStatus status;
    private Long proposedByUserId;
    private Long approvedByUserId;
    private LocalDateTime approvedAt;
    private Long cancelledByUserId;
    private LocalDateTime cancelledAt;
    private String cancelReason;
    private boolean fundingLimitExceededWarning;
    private String fundingLimitWarningMessage;

    public static TransactionResponse from(Transaction t) {
        return from(t, null);
    }

    public static TransactionResponse from(Transaction t, String fundingLimitWarningMessage) {
        TransactionResponse r = new TransactionResponse();
        r.id = t.getId();
        r.date = t.getDate();
        r.amount = t.getAmount();
        r.type = t.getType();
        r.direction = t.getDirection();
        r.description = t.getDescription();
        r.budgetItemId = t.getBudgetItem().getId();
        r.seasonId = t.getSeason().getId();
        r.plannedTransactionId = t.getPlannedTransaction() != null ? t.getPlannedTransaction().getId() : null;
        r.status = t.getStatus();
        r.proposedByUserId = t.getProposedBy() != null ? t.getProposedBy().getId() : null;
        r.approvedByUserId = t.getApprovedBy() != null ? t.getApprovedBy().getId() : null;
        r.approvedAt = t.getApprovedAt();
        r.cancelledByUserId = t.getCancelledBy() != null ? t.getCancelledBy().getId() : null;
        r.cancelledAt = t.getCancelledAt();
        r.cancelReason = t.getCancelReason();
        r.fundingLimitWarningMessage = fundingLimitWarningMessage;
        r.fundingLimitExceededWarning = fundingLimitWarningMessage != null;
        return r;
    }
}
