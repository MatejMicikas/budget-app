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

    @Column(name = "old_value_json", columnDefinition = "TEXT")
    private String oldValueJson;

    @Column(name = "new_value_json", columnDefinition = "TEXT")
    private String newValueJson;

    public enum OperationType {
        SEASON_CLOSED,
        TRANSACTION_CREATED,
        TRANSACTION_DELETED,
        BUDGET_ITEM_CREATED,
        BUDGET_ITEM_UPDATED,
        BUDGET_ITEM_DELETED,
        USER_CREATED,
        USER_ROLE_CHANGED,
        USER_TEAM_ASSIGNED,
        USER_TEAM_UNASSIGNED,
        FUNDING_SOURCE_CREATED,
        FUNDING_SOURCE_UPDATED,
        FUNDING_SOURCE_DELETED,
        TEAM_CREATED,
        TEAM_UPDATED,
        TEAM_DELETED,
        BUDGET_ITEM_TEAM_ASSIGNED,
        BUDGET_ITEM_TEAM_UNASSIGNED,
        TRANSACTION_UPDATED,
        TRANSACTION_APPROVED,
        TRANSACTION_REJECTED,
        TRANSACTION_CANCELLED,
        SEASON_CREATED,
        SEASON_UPDATED
    }

    public enum EntityType {
        SEASON,
        TRANSACTION,
        BUDGET_ITEM,
        USER,
        FUNDING_SOURCE,
        TEAM
    }
}
