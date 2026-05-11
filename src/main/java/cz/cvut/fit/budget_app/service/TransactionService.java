package cz.cvut.fit.budget_app.service;

import cz.cvut.fit.budget_app.dto.request.CancelTransactionRequest;
import cz.cvut.fit.budget_app.dto.request.CreateTransactionRequest;
import cz.cvut.fit.budget_app.dto.request.RealizePlannedTransactionRequest;
import cz.cvut.fit.budget_app.dto.request.UpdateTransactionRequest;
import cz.cvut.fit.budget_app.dto.response.TransactionResponse;
import cz.cvut.fit.budget_app.entity.BudgetItem;
import cz.cvut.fit.budget_app.entity.Transaction;
import cz.cvut.fit.budget_app.entity.User;
import cz.cvut.fit.budget_app.exception.ResourceNotFoundException;
import cz.cvut.fit.budget_app.repository.TransactionRepository;
import cz.cvut.fit.budget_app.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final BudgetItemService budgetItemService;
    private final SeasonService seasonService;
    private final FundingSourceService fundingSourceService;
    private final AuditLogService auditLogService;
    private final UserRepository userRepository;

    @Transactional
    public TransactionResponse create(CreateTransactionRequest request,
                                      Long performedByUserId,
                                      User.Role requesterRole,
                                      Long requesterTeamId) {
        BudgetItem budgetItem = budgetItemService.getOrThrow(request.getBudgetItemId());
        if (!budgetItem.getSeason().getId().equals(request.getSeasonId())) {
            throw new IllegalArgumentException(
                    "Transaction seasonId does not match BudgetItem season");
        }
        seasonService.requireOpen(budgetItem.getSeason());

        // direction must match budget item type
        Transaction.Direction expectedDirection = budgetItem.getType() == BudgetItem.ItemType.INCOME
                ? Transaction.Direction.INCOME
                : Transaction.Direction.EXPENSE;
        if (request.getDirection() != expectedDirection) {
            throw new IllegalArgumentException(
                    "Transaction direction " + request.getDirection()
                            + " does not match BudgetItem type " + budgetItem.getType());
        }
        validateDateWithinSeason(request.getDate(), budgetItem);

        User performer = userRepository.findById(performedByUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User", performedByUserId));

        if (requesterRole == User.Role.TEAM_LEADER) {
            if (request.getType() != Transaction.TransactionType.PLANNED) {
                throw new IllegalArgumentException("TEAM_LEADER can only propose PLANNED transactions");
            }
            if (requesterTeamId == null) {
                throw new IllegalStateException("TEAM_LEADER must belong to a team");
            }
            if (budgetItem.getTeam() == null || !budgetItem.getTeam().getId().equals(requesterTeamId)) {
                throw new IllegalArgumentException("TEAM_LEADER can only create transactions for own team");
            }
        }

        Transaction tx = new Transaction();
        tx.setDate(request.getDate());
        tx.setAmount(request.getAmount());
        tx.setType(request.getType());
        tx.setDirection(request.getDirection());
        tx.setDescription(request.getDescription());
        tx.setBudgetItem(budgetItem);
        tx.setSeason(budgetItem.getSeason());
        if (requesterRole == User.Role.TEAM_LEADER) {
            tx.setStatus(Transaction.ApprovalStatus.PROPOSED);
            tx.setProposedBy(performer);
            tx.setApprovedBy(null);
            tx.setApprovedAt(null);
        } else {
            tx.setStatus(Transaction.ApprovalStatus.APPROVED);
            tx.setProposedBy(performer);
            tx.setApprovedBy(performer);
            tx.setApprovedAt(LocalDateTime.now());
        }

        Transaction saved = AuditExecutor.execute(
                auditLogService,
                cz.cvut.fit.budget_app.entity.AuditLog.OperationType.TRANSACTION_CREATED,
                cz.cvut.fit.budget_app.entity.AuditLog.EntityType.TRANSACTION,
                performedByUserId,
                null,
                () -> transactionRepository.save(tx),
                Transaction::getId,
                this::transactionSnapshotJson
        );

        String fundingLimitWarning = null;
        if (budgetItem.getFundingSource() != null) {
            fundingLimitWarning = fundingSourceService.getSpendingLimitWarning(budgetItem.getFundingSource().getId());
        }

        return TransactionResponse.from(saved, fundingLimitWarning);
    }

    /**
     * Corrects date, amount, description, and optionally the budget item link (only for PROPOSED/DRAFT).
     * Does not change PLANNED vs ACTUAL or approval status — use approve/reject/realize endpoints.
     */
    @Transactional
    public TransactionResponse update(Long id, UpdateTransactionRequest request, Long performedByUserId) {
        Transaction tx = getOrThrow(id);
        seasonService.requireOpen(tx.getSeason());

        if (tx.getStatus() == Transaction.ApprovalStatus.REJECTED
                || tx.getStatus() == Transaction.ApprovalStatus.CANCELLED) {
            throw new IllegalStateException("Transaction cannot be updated in its current status");
        }

        BudgetItem budgetItem = resolveTargetBudgetItem(tx, request);
        validateDateWithinSeason(request.getDate(), budgetItem);

        String oldValueJson = transactionSnapshotJson(tx);

        tx.setDate(request.getDate());
        tx.setAmount(request.getAmount());
        tx.setDescription(request.getDescription());
        tx.setBudgetItem(budgetItem);
        tx.setSeason(budgetItem.getSeason());

        Transaction saved = AuditExecutor.execute(
                auditLogService,
                cz.cvut.fit.budget_app.entity.AuditLog.OperationType.TRANSACTION_UPDATED,
                cz.cvut.fit.budget_app.entity.AuditLog.EntityType.TRANSACTION,
                performedByUserId,
                oldValueJson,
                () -> transactionRepository.save(tx),
                Transaction::getId,
                this::transactionSnapshotJson
        );

        String fundingLimitWarning = null;
        if (budgetItem.getFundingSource() != null) {
            fundingLimitWarning = fundingSourceService.getSpendingLimitWarning(budgetItem.getFundingSource().getId());
        }

        return TransactionResponse.from(saved, fundingLimitWarning);
    }

    @Transactional
    public TransactionResponse approve(Long id, Long approverUserId) {
        Transaction tx = getOrThrow(id);
        seasonService.requireOpen(tx.getSeason());
        if (tx.getStatus() == Transaction.ApprovalStatus.CANCELLED) {
            throw new IllegalStateException("Cancelled transactions cannot be approved");
        }
        if (tx.getType() != Transaction.TransactionType.PLANNED) {
            throw new IllegalStateException("Only PLANNED transactions can be approved");
        }
        if (tx.getStatus() != Transaction.ApprovalStatus.PROPOSED) {
            throw new IllegalStateException("Only PROPOSED transactions can be approved");
        }

        String oldValueJson = AuditSnapshotSerializer.toJson(Map.of("status", tx.getStatus()));
        User approver = userRepository.findById(approverUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User", approverUserId));
        tx.setStatus(Transaction.ApprovalStatus.APPROVED);
        tx.setApprovedBy(approver);
        tx.setApprovedAt(LocalDateTime.now());
        Transaction saved = AuditExecutor.execute(
                auditLogService,
                cz.cvut.fit.budget_app.entity.AuditLog.OperationType.TRANSACTION_APPROVED,
                cz.cvut.fit.budget_app.entity.AuditLog.EntityType.TRANSACTION,
                approverUserId,
                oldValueJson,
                () -> transactionRepository.save(tx),
                Transaction::getId,
                this::approvalSnapshotJson
        );
        return TransactionResponse.from(saved);
    }

    @Transactional
    public TransactionResponse reject(Long id, Long reviewerUserId) {
        Transaction tx = getOrThrow(id);
        seasonService.requireOpen(tx.getSeason());
        if (tx.getStatus() == Transaction.ApprovalStatus.CANCELLED) {
            throw new IllegalStateException("Cancelled transactions cannot be rejected");
        }
        if (tx.getType() != Transaction.TransactionType.PLANNED) {
            throw new IllegalStateException("Only PLANNED transactions can be rejected");
        }
        if (tx.getStatus() != Transaction.ApprovalStatus.PROPOSED) {
            throw new IllegalStateException("Only PROPOSED transactions can be rejected");
        }

        String oldValueJson = AuditSnapshotSerializer.toJson(Map.of("status", tx.getStatus()));
        User reviewer = userRepository.findById(reviewerUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User", reviewerUserId));
        tx.setStatus(Transaction.ApprovalStatus.REJECTED);
        tx.setApprovedBy(reviewer);
        tx.setApprovedAt(LocalDateTime.now());
        Transaction saved = AuditExecutor.execute(
                auditLogService,
                cz.cvut.fit.budget_app.entity.AuditLog.OperationType.TRANSACTION_REJECTED,
                cz.cvut.fit.budget_app.entity.AuditLog.EntityType.TRANSACTION,
                reviewerUserId,
                oldValueJson,
                () -> transactionRepository.save(tx),
                Transaction::getId,
                this::approvalSnapshotJson
        );
        return TransactionResponse.from(saved);
    }

    @Transactional
    public TransactionResponse realizePlanned(Long plannedTransactionId,
                                              RealizePlannedTransactionRequest request,
                                              Long reviewerUserId) {
        Transaction planned = getOrThrow(plannedTransactionId);
        seasonService.requireOpen(planned.getSeason());
        if (planned.getStatus() == Transaction.ApprovalStatus.CANCELLED) {
            throw new IllegalStateException("Cannot realize a cancelled planned transaction");
        }
        if (planned.getType() != Transaction.TransactionType.PLANNED) {
            throw new IllegalStateException("Only PLANNED transactions can be realized");
        }
        if (planned.getStatus() != Transaction.ApprovalStatus.APPROVED) {
            throw new IllegalStateException("Only APPROVED planned transactions can be realized");
        }
        validateDateWithinSeason(request.getDate(), planned.getBudgetItem());

        User reviewer = userRepository.findById(reviewerUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User", reviewerUserId));

        Transaction actual = new Transaction();
        actual.setDate(request.getDate());
        actual.setAmount(request.getAmount());
        actual.setType(Transaction.TransactionType.ACTUAL);
        actual.setDirection(planned.getDirection());
        actual.setDescription(request.getDescription());
        actual.setBudgetItem(planned.getBudgetItem());
        actual.setSeason(planned.getSeason());
        actual.setPlannedTransaction(planned);
        actual.setStatus(Transaction.ApprovalStatus.APPROVED);
        actual.setProposedBy(reviewer);
        actual.setApprovedBy(reviewer);
        actual.setApprovedAt(LocalDateTime.now());

        Transaction saved = AuditExecutor.execute(
                auditLogService,
                cz.cvut.fit.budget_app.entity.AuditLog.OperationType.TRANSACTION_CREATED,
                cz.cvut.fit.budget_app.entity.AuditLog.EntityType.TRANSACTION,
                reviewerUserId,
                null,
                () -> transactionRepository.save(actual),
                Transaction::getId,
                this::transactionSnapshotJson
        );
        String fundingLimitWarning = null;
        if (planned.getBudgetItem().getFundingSource() != null) {
            fundingLimitWarning = fundingSourceService.getSpendingLimitWarning(
                    planned.getBudgetItem().getFundingSource().getId());
        }
        return TransactionResponse.from(saved, fundingLimitWarning);
    }

    @Transactional(readOnly = true)
    public TransactionResponse findById(Long id, User.Role requesterRole, Long requesterTeamId) {
        Transaction tx = getOrThrow(id);
        if (requesterRole == User.Role.TEAM_LEADER) {
            if (requesterTeamId == null || tx.getBudgetItem().getTeam() == null
                    || !requesterTeamId.equals(tx.getBudgetItem().getTeam().getId())) {
                throw new ResourceNotFoundException("Transaction", id);
            }
        }
        return TransactionResponse.from(tx);
    }

    @Transactional(readOnly = true)
    public List<TransactionResponse> findBySeasonId(Long seasonId) {
        return transactionRepository.findBySeasonId(seasonId).stream()
                .map(TransactionResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<TransactionResponse> findBySeasonWithFilters(Long seasonId,
                                                             Transaction.TransactionType type,
                                                             Long budgetItemId,
                                                             User.Role requesterRole,
                                                             Long requesterTeamId) {
        if (requesterRole == User.Role.TEAM_LEADER && requesterTeamId == null) {
            throw new IllegalStateException("TEAM_LEADER must belong to a team");
        }
        List<Transaction> transactions;
        boolean teamScoped = requesterRole == User.Role.TEAM_LEADER;
        if (type != null && budgetItemId != null) {
            transactions = teamScoped
                    ? transactionRepository.findBySeasonIdAndTypeAndBudgetItemIdAndBudgetItemTeamId(
                    seasonId, type, budgetItemId, requesterTeamId)
                    : transactionRepository.findBySeasonIdAndTypeAndBudgetItemId(seasonId, type, budgetItemId);
        } else if (type != null) {
            transactions = teamScoped
                    ? transactionRepository.findBySeasonIdAndTypeAndBudgetItemTeamId(seasonId, type, requesterTeamId)
                    : transactionRepository.findBySeasonIdAndType(seasonId, type);
        } else if (budgetItemId != null) {
            transactions = teamScoped
                    ? transactionRepository.findBySeasonIdAndBudgetItemIdAndBudgetItemTeamId(
                    seasonId, budgetItemId, requesterTeamId)
                    : transactionRepository.findBySeasonIdAndBudgetItemId(seasonId, budgetItemId);
        } else {
            transactions = teamScoped
                    ? transactionRepository.findBySeasonIdAndBudgetItemTeamId(seasonId, requesterTeamId)
                    : transactionRepository.findBySeasonId(seasonId);
        }

        return transactions.stream()
                .map(TransactionResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<TransactionResponse> findByBudgetItemId(Long budgetItemId) {
        return transactionRepository.findByBudgetItemId(budgetItemId).stream()
                .map(TransactionResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<TransactionResponse> findByFundingSourceId(Long fundingSourceId,
                                                           User.Role requesterRole,
                                                           Long requesterTeamId) {
        if (requesterRole == User.Role.TEAM_LEADER && requesterTeamId == null) {
            throw new IllegalStateException("TEAM_LEADER must belong to a team");
        }
        List<Transaction> transactions = requesterRole == User.Role.TEAM_LEADER
                ? transactionRepository.findByBudgetItemFundingSourceIdAndBudgetItemTeamId(fundingSourceId, requesterTeamId)
                : transactionRepository.findByBudgetItemFundingSourceId(fundingSourceId);
        return transactions.stream()
                .map(TransactionResponse::from)
                .toList();
    }

    /**
     * Soft-cancel: sets {@link Transaction.ApprovalStatus#CANCELLED} with audit metadata.
     * Does not remove the row. Cannot cancel {@code REJECTED} records. Cannot cancel an
     * {@code APPROVED} {@code PLANNED} transaction if realized {@code ACTUAL} rows exist.
     */
    @Transactional
    public TransactionResponse cancel(Long id, CancelTransactionRequest request, Long performedByUserId) {
        Transaction tx = getOrThrow(id);
        seasonService.requireOpen(tx.getSeason());

        if (tx.getStatus() == Transaction.ApprovalStatus.CANCELLED) {
            throw new IllegalStateException("Transaction is already cancelled");
        }
        if (tx.getStatus() == Transaction.ApprovalStatus.REJECTED) {
            throw new IllegalStateException("Rejected transactions cannot be cancelled");
        }

        if (tx.getType() == Transaction.TransactionType.PLANNED
                && tx.getStatus() == Transaction.ApprovalStatus.APPROVED
                && transactionRepository.existsByPlannedTransaction_Id(tx.getId())) {
            throw new IllegalStateException(
                    "Cannot cancel this planned transaction because realized actual transaction(s) exist; cancel those first");
        }

        User canceller = userRepository.findById(performedByUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User", performedByUserId));

        String oldValueJson = AuditSnapshotSerializer.toJson(Map.of("status", tx.getStatus()));
        tx.setStatus(Transaction.ApprovalStatus.CANCELLED);
        tx.setCancelledBy(canceller);
        tx.setCancelledAt(LocalDateTime.now());
        tx.setCancelReason(request.getReason());

        Transaction saved = AuditExecutor.execute(
                auditLogService,
                cz.cvut.fit.budget_app.entity.AuditLog.OperationType.TRANSACTION_CANCELLED,
                cz.cvut.fit.budget_app.entity.AuditLog.EntityType.TRANSACTION,
                performedByUserId,
                oldValueJson,
                () -> transactionRepository.save(tx),
                Transaction::getId,
                this::cancelSnapshotJson
        );
        return TransactionResponse.from(saved);
    }

    /**
     * Hard-delete for treasurer/admin workflow. Writes {@link cz.cvut.fit.budget_app.entity.AuditLog.OperationType#TRANSACTION_DELETED}
     * with a snapshot of the row in {@code oldValueJson}.
     * <p>
     * Cannot delete an approved planned transaction while realized {@code ACTUAL} rows still reference it
     * (same constraint as {@link #cancel(Long, CancelTransactionRequest, Long)}).
     */
    @Transactional
    public void delete(Long id, Long performedByUserId) {
        Transaction tx = getOrThrow(id);
        seasonService.requireOpen(tx.getSeason());

        if (tx.getType() == Transaction.TransactionType.PLANNED
                && tx.getStatus() == Transaction.ApprovalStatus.APPROVED
                && transactionRepository.existsByPlannedTransaction_Id(tx.getId())) {
            throw new IllegalStateException(
                    "Cannot delete this planned transaction because realized actual transaction(s) exist; delete those first");
        }

        String oldValueJson = transactionSnapshotJson(tx);
        Long transactionId = tx.getId();

        AuditExecutor.execute(
                auditLogService,
                cz.cvut.fit.budget_app.entity.AuditLog.OperationType.TRANSACTION_DELETED,
                cz.cvut.fit.budget_app.entity.AuditLog.EntityType.TRANSACTION,
                transactionId,
                performedByUserId,
                oldValueJson,
                () -> {
                    transactionRepository.delete(tx);
                    return null;
                });
    }

    private String transactionSnapshotJson(Transaction tx) {
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("date", tx.getDate());
        snapshot.put("amount", tx.getAmount());
        snapshot.put("type", tx.getType());
        snapshot.put("direction", tx.getDirection());
        snapshot.put("status", tx.getStatus());
        snapshot.put("budgetItemId", tx.getBudgetItem() != null ? tx.getBudgetItem().getId() : null);
        return AuditSnapshotSerializer.toJson(snapshot);
    }

    private String approvalSnapshotJson(Transaction tx) {
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("status", tx.getStatus());
        snapshot.put("approvedBy", tx.getApprovedBy() != null ? tx.getApprovedBy().getId() : null);
        snapshot.put("approvedAt", tx.getApprovedAt());
        return AuditSnapshotSerializer.toJson(snapshot);
    }

    private String cancelSnapshotJson(Transaction tx) {
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("status", tx.getStatus());
        snapshot.put("cancelledBy", tx.getCancelledBy() != null ? tx.getCancelledBy().getId() : null);
        snapshot.put("cancelledAt", tx.getCancelledAt());
        snapshot.put("cancelReason", tx.getCancelReason());
        return AuditSnapshotSerializer.toJson(snapshot);
    }

    private Transaction getOrThrow(Long id) {
        return transactionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction", id));
    }

    private void validateDateWithinSeason(java.time.LocalDate transactionDate, BudgetItem budgetItem) {
        if (transactionDate.isBefore(budgetItem.getSeason().getDateFrom())
                || transactionDate.isAfter(budgetItem.getSeason().getDateTo())) {
            throw new IllegalArgumentException("Transaction date must be within season range");
        }
    }

    /**
     * Keeps current budget item unless {@code request.budgetItemId} points to another line;
     * rebinding is restricted to workflow rules (see javadoc on {@link UpdateTransactionRequest}).
     */
    private BudgetItem resolveTargetBudgetItem(Transaction tx, UpdateTransactionRequest request) {
        Long requestedBudgetItemId = request.getBudgetItemId();
        if (requestedBudgetItemId == null
                || requestedBudgetItemId.equals(tx.getBudgetItem().getId())) {
            return tx.getBudgetItem();
        }

        if (tx.getStatus() == Transaction.ApprovalStatus.APPROVED) {
            throw new IllegalStateException(
                    "Cannot reassign budget item on an approved transaction; use workflow endpoints or delete and recreate");
        }
        if (tx.getStatus() != Transaction.ApprovalStatus.PROPOSED
                && tx.getStatus() != Transaction.ApprovalStatus.DRAFT) {
            throw new IllegalStateException("Budget item can only be changed for DRAFT or PROPOSED transactions");
        }

        BudgetItem candidate = budgetItemService.getOrThrow(requestedBudgetItemId);
        if (!candidate.getSeason().getId().equals(tx.getSeason().getId())) {
            throw new IllegalArgumentException("Budget item must belong to the same season as the transaction");
        }
        seasonService.requireOpen(candidate.getSeason());

        Transaction.Direction expectedDirection = candidate.getType() == BudgetItem.ItemType.INCOME
                ? Transaction.Direction.INCOME
                : Transaction.Direction.EXPENSE;
        if (tx.getDirection() != expectedDirection) {
            throw new IllegalArgumentException(
                    "Transaction direction does not match the selected budget item type");
        }
        return candidate;
    }
}

