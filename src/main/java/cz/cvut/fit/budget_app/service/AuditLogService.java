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
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;
    private final UserRepository userRepository;

    @Transactional
    public void log(AuditLog.OperationType operationType,
                    AuditLog.EntityType affectedEntityType,
                    Long affectedEntityId,
                    Long performedByUserId,
                    String oldValueJson,
                    String newValueJson) {
        persist(operationType, affectedEntityType, affectedEntityId, performedByUserId, oldValueJson, newValueJson);
    }

    @Transactional
    public void logSeasonClosed(Long seasonId, Long performedByUserId) {
        log(AuditLog.OperationType.SEASON_CLOSED,
                AuditLog.EntityType.SEASON,
                seasonId,
                performedByUserId,
                null,
                AuditSnapshotSerializer.toJson(Map.of("status", "CLOSED")));
    }

    @Transactional
    public void logSeasonCreated(Long seasonId, Long performedByUserId, String newValueJson) {
        persist(
                AuditLog.OperationType.SEASON_CREATED,
                AuditLog.EntityType.SEASON,
                seasonId,
                performedByUserId,
                null,
                newValueJson
        );
    }

    @Transactional
    public void logSeasonUpdated(Long seasonId, Long performedByUserId, String oldValueJson, String newValueJson) {
        persist(
                AuditLog.OperationType.SEASON_UPDATED,
                AuditLog.EntityType.SEASON,
                seasonId,
                performedByUserId,
                oldValueJson,
                newValueJson
        );
    }

    @Transactional
    public void logTransactionCreated(Long transactionId, Long performedByUserId) {
        persist(
                AuditLog.OperationType.TRANSACTION_CREATED,
                AuditLog.EntityType.TRANSACTION,
                transactionId,
                performedByUserId,
                null,
                null
        );
    }

    @Transactional
    public void logTransactionUpdated(Long transactionId, Long performedByUserId, String oldValueJson, String newValueJson) {
        persist(
                AuditLog.OperationType.TRANSACTION_UPDATED,
                AuditLog.EntityType.TRANSACTION,
                transactionId,
                performedByUserId,
                oldValueJson,
                newValueJson
        );
    }

    @Transactional
    public void logTransactionApproved(Long transactionId, Long performedByUserId, String oldValueJson, String newValueJson) {
        persist(
                AuditLog.OperationType.TRANSACTION_APPROVED,
                AuditLog.EntityType.TRANSACTION,
                transactionId,
                performedByUserId,
                oldValueJson,
                newValueJson
        );
    }

    @Transactional
    public void logTransactionRejected(Long transactionId, Long performedByUserId, String oldValueJson, String newValueJson) {
        persist(
                AuditLog.OperationType.TRANSACTION_REJECTED,
                AuditLog.EntityType.TRANSACTION,
                transactionId,
                performedByUserId,
                oldValueJson,
                newValueJson
        );
    }

    @Transactional
    public void logTransactionDeleted(Long transactionId, Long performedByUserId) {
        logTransactionDeleted(transactionId, performedByUserId, null);
    }

    @Transactional
    public void logTransactionDeleted(Long transactionId, Long performedByUserId, String oldValueJson) {
        persist(
                AuditLog.OperationType.TRANSACTION_DELETED,
                AuditLog.EntityType.TRANSACTION,
                transactionId,
                performedByUserId,
                oldValueJson,
                null
        );
    }

    @Transactional
    public void logTransactionCancelled(Long transactionId,
                                        Long performedByUserId,
                                        String oldValueJson,
                                        String newValueJson) {
        persist(
                AuditLog.OperationType.TRANSACTION_CANCELLED,
                AuditLog.EntityType.TRANSACTION,
                transactionId,
                performedByUserId,
                oldValueJson,
                newValueJson
        );
    }

    @Transactional
    public void logBudgetItemCreated(Long budgetItemId, Long performedByUserId) {
        persist(
                AuditLog.OperationType.BUDGET_ITEM_CREATED,
                AuditLog.EntityType.BUDGET_ITEM,
                budgetItemId,
                performedByUserId,
                null,
                null
        );
    }

    @Transactional
    public void logBudgetItemUpdated(Long budgetItemId, Long performedByUserId, String oldValueJson, String newValueJson) {
        persist(
                AuditLog.OperationType.BUDGET_ITEM_UPDATED,
                AuditLog.EntityType.BUDGET_ITEM,
                budgetItemId,
                performedByUserId,
                oldValueJson,
                newValueJson
        );
    }

    @Transactional
    public void logBudgetItemDeleted(Long budgetItemId, Long performedByUserId) {
        logBudgetItemDeleted(budgetItemId, performedByUserId, null);
    }

    @Transactional
    public void logBudgetItemDeleted(Long budgetItemId, Long performedByUserId, String oldValueJson) {
        persist(
                AuditLog.OperationType.BUDGET_ITEM_DELETED,
                AuditLog.EntityType.BUDGET_ITEM,
                budgetItemId,
                performedByUserId,
                oldValueJson,
                null
        );
    }

    @Transactional
    public void logUserCreated(Long userId, Long performedByUserId, String newValueJson) {
        persist(
                AuditLog.OperationType.USER_CREATED,
                AuditLog.EntityType.USER,
                userId,
                performedByUserId,
                null,
                newValueJson
        );
    }

    @Transactional
    public void logUserRoleChanged(Long userId, Long performedByUserId, String oldValueJson, String newValueJson) {
        persist(
                AuditLog.OperationType.USER_ROLE_CHANGED,
                AuditLog.EntityType.USER,
                userId,
                performedByUserId,
                oldValueJson,
                newValueJson
        );
    }

    @Transactional
    public void logUserTeamAssigned(Long userId, Long performedByUserId, String oldValueJson, String newValueJson) {
        persist(
                AuditLog.OperationType.USER_TEAM_ASSIGNED,
                AuditLog.EntityType.USER,
                userId,
                performedByUserId,
                oldValueJson,
                newValueJson
        );
    }

    @Transactional
    public void logUserTeamUnassigned(Long userId, Long performedByUserId, String oldValueJson, String newValueJson) {
        persist(
                AuditLog.OperationType.USER_TEAM_UNASSIGNED,
                AuditLog.EntityType.USER,
                userId,
                performedByUserId,
                oldValueJson,
                newValueJson
        );
    }

    @Transactional
    public void logFundingSourceCreated(Long fundingSourceId, Long performedByUserId) {
        persist(
                AuditLog.OperationType.FUNDING_SOURCE_CREATED,
                AuditLog.EntityType.FUNDING_SOURCE,
                fundingSourceId,
                performedByUserId,
                null,
                null
        );
    }

    @Transactional
    public void logFundingSourceUpdated(Long fundingSourceId,
                                        Long performedByUserId,
                                        String oldValueJson,
                                        String newValueJson) {
        persist(
                AuditLog.OperationType.FUNDING_SOURCE_UPDATED,
                AuditLog.EntityType.FUNDING_SOURCE,
                fundingSourceId,
                performedByUserId,
                oldValueJson,
                newValueJson
        );
    }

    @Transactional
    public void logFundingSourceDeleted(Long fundingSourceId, Long performedByUserId) {
        logFundingSourceDeleted(fundingSourceId, performedByUserId, null);
    }

    @Transactional
    public void logFundingSourceDeleted(Long fundingSourceId, Long performedByUserId, String oldValueJson) {
        persist(
                AuditLog.OperationType.FUNDING_SOURCE_DELETED,
                AuditLog.EntityType.FUNDING_SOURCE,
                fundingSourceId,
                performedByUserId,
                oldValueJson,
                null
        );
    }

    @Transactional
    public void logTeamCreated(Long teamId, Long performedByUserId, String newValueJson) {
        persist(
                AuditLog.OperationType.TEAM_CREATED,
                AuditLog.EntityType.TEAM,
                teamId,
                performedByUserId,
                null,
                newValueJson
        );
    }

    @Transactional
    public void logTeamUpdated(Long teamId, Long performedByUserId, String oldValueJson, String newValueJson) {
        persist(
                AuditLog.OperationType.TEAM_UPDATED,
                AuditLog.EntityType.TEAM,
                teamId,
                performedByUserId,
                oldValueJson,
                newValueJson
        );
    }

    @Transactional
    public void logTeamDeleted(Long teamId, Long performedByUserId, String oldValueJson) {
        persist(
                AuditLog.OperationType.TEAM_DELETED,
                AuditLog.EntityType.TEAM,
                teamId,
                performedByUserId,
                oldValueJson,
                null
        );
    }

    @Transactional
    public void logBudgetItemTeamAssigned(Long budgetItemId, Long performedByUserId, String oldValueJson, String newValueJson) {
        persist(
                AuditLog.OperationType.BUDGET_ITEM_TEAM_ASSIGNED,
                AuditLog.EntityType.BUDGET_ITEM,
                budgetItemId,
                performedByUserId,
                oldValueJson,
                newValueJson
        );
    }

    @Transactional
    public void logBudgetItemTeamUnassigned(Long budgetItemId, Long performedByUserId, String oldValueJson, String newValueJson) {
        persist(
                AuditLog.OperationType.BUDGET_ITEM_TEAM_UNASSIGNED,
                AuditLog.EntityType.BUDGET_ITEM,
                budgetItemId,
                performedByUserId,
                oldValueJson,
                newValueJson
        );
    }

    private void persist(AuditLog.OperationType operationType,
                         AuditLog.EntityType affectedEntityType,
                         Long affectedEntityId,
                         Long performedByUserId,
                         String oldValueJson,
                         String newValueJson) {
        User performer = userRepository.findById(performedByUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User", performedByUserId));

        AuditLog entry = new AuditLog();
        entry.setTimestamp(LocalDateTime.now());
        entry.setOperationType(operationType);
        entry.setAffectedEntityType(affectedEntityType);
        entry.setAffectedEntityId(affectedEntityId);
        entry.setPerformedBy(performer);
        entry.setOldValueJson(oldValueJson);
        entry.setNewValueJson(newValueJson);

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

    @Transactional(readOnly = true)
    public List<AuditLogResponse> findByOperationTypeAndDateRange(
            AuditLog.OperationType operationType,
            LocalDateTime from,
            LocalDateTime to) {
        return auditLogRepository.findByOperationTypeAndTimestampBetween(operationType, from, to).stream()
                .map(AuditLogResponse::from)
                .toList();
    }
}
