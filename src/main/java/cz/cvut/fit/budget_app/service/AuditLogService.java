package cz.cvut.fit.budget_app.service;

import cz.cvut.fit.budget_app.dto.response.AuditLogResponse;
import cz.cvut.fit.budget_app.entity.AuditLog;
import cz.cvut.fit.budget_app.entity.User;
import cz.cvut.fit.budget_app.exception.ResourceNotFoundException;
import cz.cvut.fit.budget_app.repository.AuditLogRepository;
import cz.cvut.fit.budget_app.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;
    private final UserRepository userRepository;

    @Transactional
    public void logSeasonClosed(Long seasonId, Long performedByUserId) {
        persist(
                AuditLog.OperationType.SEASON_CLOSED,
                AuditLog.EntityType.SEASON,
                seasonId,
                performedByUserId
        );
    }

    @Transactional
    public void logTransactionCreated(Long transactionId, Long performedByUserId) {
        persist(
                AuditLog.OperationType.TRANSACTION_CREATED,
                AuditLog.EntityType.TRANSACTION,
                transactionId,
                performedByUserId
        );
    }

    @Transactional
    public void logTransactionDeleted(Long transactionId, Long performedByUserId) {
        persist(
                AuditLog.OperationType.TRANSACTION_DELETED,
                AuditLog.EntityType.TRANSACTION,
                transactionId,
                performedByUserId
        );
    }

    @Transactional
    public void logBudgetItemCreated(Long budgetItemId, Long performedByUserId) {
        persist(
                AuditLog.OperationType.BUDGET_ITEM_CREATED,
                AuditLog.EntityType.BUDGET_ITEM,
                budgetItemId,
                performedByUserId
        );
    }

    @Transactional
    public void logBudgetItemDeleted(Long budgetItemId, Long performedByUserId) {
        persist(
                AuditLog.OperationType.BUDGET_ITEM_DELETED,
                AuditLog.EntityType.BUDGET_ITEM,
                budgetItemId,
                performedByUserId
        );
    }

    @Transactional
    public void logUserRoleChanged(Long userId, Long performedByUserId) {
        persist(
                AuditLog.OperationType.USER_ROLE_CHANGED,
                AuditLog.EntityType.USER,
                userId,
                performedByUserId
        );
    }

    @Transactional
    public void logFundingSourceCreated(Long fundingSourceId, Long performedByUserId) {
        persist(
                AuditLog.OperationType.FUNDING_SOURCE_CREATED,
                AuditLog.EntityType.FUNDING_SOURCE,
                fundingSourceId,
                performedByUserId
        );
    }

    @Transactional
    public void logFundingSourceUpdated(Long fundingSourceId, Long performedByUserId) {
        persist(
                AuditLog.OperationType.FUNDING_SOURCE_UPDATED,
                AuditLog.EntityType.FUNDING_SOURCE,
                fundingSourceId,
                performedByUserId
        );
    }

    @Transactional
    public void logFundingSourceDeleted(Long fundingSourceId, Long performedByUserId) {
        persist(
                AuditLog.OperationType.FUNDING_SOURCE_DELETED,
                AuditLog.EntityType.FUNDING_SOURCE,
                fundingSourceId,
                performedByUserId
        );
    }

    private void persist(AuditLog.OperationType operationType,
                         AuditLog.EntityType affectedEntityType,
                         Long affectedEntityId,
                         Long performedByUserId) {
        User performer = userRepository.findById(performedByUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User", performedByUserId));

        AuditLog entry = new AuditLog();
        entry.setTimestamp(LocalDateTime.now());
        entry.setOperationType(operationType);
        entry.setAffectedEntityType(affectedEntityType);
        entry.setAffectedEntityId(affectedEntityId);
        entry.setPerformedBy(performer);

        auditLogRepository.save(entry);
    }

    @Transactional(readOnly = true)
    public List<AuditLogResponse> findAll() {
        return auditLogRepository.findAll().stream()
                .map(AuditLogResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<AuditLogResponse> findByOperationType(AuditLog.OperationType operationType) {
        return auditLogRepository.findByOperationType(operationType).stream()
                .map(AuditLogResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<AuditLogResponse> findByDateRange(LocalDateTime from, LocalDateTime to) {
        return auditLogRepository.findByTimestampBetween(from, to).stream()
                .map(AuditLogResponse::from)
                .toList();
    }
}
