package cz.cvut.fit.budget_app.service;

import cz.cvut.fit.budget_app.entity.AuditLog;

import java.util.function.Function;
import java.util.function.Supplier;

final class AuditExecutor {
    private AuditExecutor() {
    }

    static <T> T execute(AuditLogService auditLogService,
                         AuditLog.OperationType operationType,
                         AuditLog.EntityType entityType,
                         Long performedByUserId,
                         String oldSnapshotJson,
                         Supplier<T> operation,
                         Function<T, Long> entityIdExtractor,
                         Function<T, String> newSnapshotExtractor) {
        T result = operation.get();
        auditLogService.log(operationType,
                entityType,
                entityIdExtractor.apply(result),
                performedByUserId,
                oldSnapshotJson,
                newSnapshotExtractor.apply(result));
        return result;
    }

    static void execute(AuditLogService auditLogService,
                        AuditLog.OperationType operationType,
                        AuditLog.EntityType entityType,
                        Long entityId,
                        Long performedByUserId,
                        String oldSnapshotJson,
                        Supplier<Void> operation) {
        operation.get();
        auditLogService.log(operationType, entityType, entityId, performedByUserId, oldSnapshotJson, null);
    }
}
