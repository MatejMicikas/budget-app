package cz.cvut.fit.budget_app.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "transactions")
@Data
@NoArgsConstructor
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private LocalDate date;

    @Column(nullable = false)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TransactionType type;      // PLANNED / ACTUAL

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Direction direction;       // INCOME / EXPENSE

    private String description;

    @ManyToOne(optional = false)
    @JoinColumn(name = "budget_item_id", nullable = false)
    private BudgetItem budgetItem;

    @ManyToOne(optional = false)
    @JoinColumn(name = "season_id", nullable = false)
    private Season season;

    @ManyToOne
    @JoinColumn(name = "planned_transaction_id")
    private Transaction plannedTransaction;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ApprovalStatus status = ApprovalStatus.DRAFT;

    @ManyToOne
    @JoinColumn(name = "proposed_by")
    private User proposedBy;

    @ManyToOne
    @JoinColumn(name = "approved_by")
    private User approvedBy;

    @Column(name = "approved_at")
    private LocalDateTime approvedAt;

    @ManyToOne
    @JoinColumn(name = "cancelled_by")
    private User cancelledBy;

    @Column(name = "cancelled_at")
    private LocalDateTime cancelledAt;

    @Column(name = "cancel_reason", length = 2000)
    private String cancelReason;

    public enum TransactionType {
        PLANNED, ACTUAL
    }

    public enum Direction {
        INCOME, EXPENSE
    }

    public enum ApprovalStatus {
        DRAFT, PROPOSED, APPROVED, REJECTED, CANCELLED
    }
}
