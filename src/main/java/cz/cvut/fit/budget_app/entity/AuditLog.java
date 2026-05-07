package cz.cvut.fit.budget_app.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "audit_logs")
@Data
@NoArgsConstructor
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private LocalDateTime timestamp;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OperationType operationType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EntityType affectedEntityType;

    @Column(nullable = false)
    private Long affectedEntityId;

    @ManyToOne(optional = false)
    @JoinColumn(name = "performed_by", nullable = false)
    private User performedBy;

    public enum OperationType {
        SEASON_CLOSED,
        TRANSACTION_CREATED,
        TRANSACTION_DELETED,
        BUDGET_ITEM_CREATED,
        BUDGET_ITEM_DELETED,
        USER_ROLE_CHANGED,
        FUNDING_SOURCE_CREATED,
        FUNDING_SOURCE_UPDATED,
        FUNDING_SOURCE_DELETED
    }

    public enum EntityType {
        SEASON,
        TRANSACTION,
        BUDGET_ITEM,
        USER,
        FUNDING_SOURCE
    }
}
