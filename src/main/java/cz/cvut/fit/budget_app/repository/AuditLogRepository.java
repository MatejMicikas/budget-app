package cz.cvut.fit.budget_app.repository;

import cz.cvut.fit.budget_app.entity.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    List<AuditLog> findByOperationType(AuditLog.OperationType operationType);

    List<AuditLog> findByTimestampBetween(LocalDateTime start, LocalDateTime end);

    List<AuditLog> findByOperationTypeAndTimestampBetween(
            AuditLog.OperationType operationType,
            LocalDateTime start,
            LocalDateTime end);
}
