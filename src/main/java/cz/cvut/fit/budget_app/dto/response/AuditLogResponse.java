package cz.cvut.fit.budget_app.dto.response;

import cz.cvut.fit.budget_app.entity.AuditLog;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AuditLogResponse {
    private Long id;
    private LocalDateTime timestamp;
    private AuditLog.OperationType operationType;
    private AuditLog.EntityType affectedEntityType;
    private Long affectedEntityId;
    private Long performedById;
    private String performedByUsername;

    public static AuditLogResponse from(AuditLog log) {
        AuditLogResponse r = new AuditLogResponse();
        r.id = log.getId();
        r.timestamp = log.getTimestamp();
        r.operationType = log.getOperationType();
        r.affectedEntityType = log.getAffectedEntityType();
        r.affectedEntityId = log.getAffectedEntityId();
        r.performedById = log.getPerformedBy().getId();
        r.performedByUsername = log.getPerformedBy().getUsername();
        return r;
    }
}
