package cz.cvut.fit.budget_app.service;

import cz.cvut.fit.budget_app.dto.request.CreateBudgetItemRequest;
import cz.cvut.fit.budget_app.dto.request.UpdateBudgetItemRequest;
import cz.cvut.fit.budget_app.dto.response.BudgetItemResponse;
import cz.cvut.fit.budget_app.entity.BudgetItem;
import cz.cvut.fit.budget_app.entity.FundingSource;
import cz.cvut.fit.budget_app.entity.Season;
import cz.cvut.fit.budget_app.entity.Team;
import cz.cvut.fit.budget_app.entity.Transaction;
import cz.cvut.fit.budget_app.entity.User;
import cz.cvut.fit.budget_app.exception.ResourceNotFoundException;
import cz.cvut.fit.budget_app.repository.BudgetItemRepository;
import cz.cvut.fit.budget_app.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class BudgetItemService {

    private final BudgetItemRepository budgetItemRepository;
    private final TransactionRepository transactionRepository;
    private final SeasonService seasonService;
    private final FundingSourceService fundingSourceService;
    private final AuditLogService auditLogService;
    private final TeamService teamService;

    @Transactional
    public BudgetItemResponse create(CreateBudgetItemRequest request, Long performedByUserId) {
        Season season = seasonService.getOrThrow(request.getSeasonId());
        seasonService.requireOpen(season);

        FundingSource fundingSource = null;
        if (request.getFundingSourceId() != null) {
            fundingSource = fundingSourceService.getOrThrow(request.getFundingSourceId());
            if (!fundingSource.getSeason().getId().equals(season.getId())) {
                throw new IllegalArgumentException("FundingSource does not belong to the same season");
            }
        }

        BudgetItem item = new BudgetItem();
        item.setName(request.getName());
        item.setType(request.getType());
        item.setPlannedAmount(request.getPlannedAmount());
        item.setSeason(season);
        item.setFundingSource(fundingSource);

        BudgetItem saved = AuditExecutor.execute(
                auditLogService,
                cz.cvut.fit.budget_app.entity.AuditLog.OperationType.BUDGET_ITEM_CREATED,
                cz.cvut.fit.budget_app.entity.AuditLog.EntityType.BUDGET_ITEM,
                performedByUserId,
                null,
                () -> budgetItemRepository.save(item),
                BudgetItem::getId,
                this::budgetItemSnapshotJson
        );

        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public BudgetItemResponse findById(Long id, User.Role requesterRole, Long requesterTeamId) {
        BudgetItem item = getOrThrow(id);
        if (requesterRole == User.Role.TEAM_LEADER) {
            if (requesterTeamId == null) {
                throw new IllegalStateException("TEAM_LEADER must belong to a team");
            }
            if (item.getTeam() == null || !requesterTeamId.equals(item.getTeam().getId())) {
                throw new ResourceNotFoundException("BudgetItem", id);
            }
        }
        return toResponse(item);
    }

    @Transactional(readOnly = true)
    public List<BudgetItemResponse> findBySeasonId(Long seasonId, User.Role requesterRole, Long requesterTeamId) {
        List<BudgetItem> items;
        if (requesterRole == User.Role.TEAM_LEADER) {
            if (requesterTeamId == null) {
                throw new IllegalStateException("TEAM_LEADER must belong to a team");
            }
            items = budgetItemRepository.findBySeasonIdAndTeamId(seasonId, requesterTeamId);
        } else {
            items = budgetItemRepository.findBySeasonId(seasonId);
        }
        return items.stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public BudgetItemResponse update(Long id, UpdateBudgetItemRequest request, Long performedByUserId) {
        BudgetItem item = getOrThrow(id);
        seasonService.requireOpen(item.getSeason());
        if (!request.getType().equals(item.getType())
                && transactionRepository.existsByBudgetItem_Id(id)) {
            throw new IllegalStateException(
                    "Cannot change budget item type because transactions already exist for this line");
        }

        String oldValueJson = budgetItemSnapshotJson(item);

        FundingSource fundingSource = null;
        if (request.getFundingSourceId() != null) {
            fundingSource = fundingSourceService.getOrThrow(request.getFundingSourceId());
            if (!fundingSource.getSeason().getId().equals(item.getSeason().getId())) {
                throw new IllegalArgumentException("FundingSource does not belong to the same season");
            }
        }

        item.setName(request.getName());
        item.setType(request.getType());
        item.setPlannedAmount(request.getPlannedAmount());
        item.setFundingSource(fundingSource);

        BudgetItem saved = AuditExecutor.execute(
                auditLogService,
                cz.cvut.fit.budget_app.entity.AuditLog.OperationType.BUDGET_ITEM_UPDATED,
                cz.cvut.fit.budget_app.entity.AuditLog.EntityType.BUDGET_ITEM,
                performedByUserId,
                oldValueJson,
                () -> budgetItemRepository.save(item),
                BudgetItem::getId,
                this::budgetItemSnapshotJson
        );
        return toResponse(saved);
    }

    @Transactional
    public void delete(Long id, Long performedByUserId) {
        BudgetItem item = getOrThrow(id);
        seasonService.requireOpen(item.getSeason());
        String oldValueJson = budgetItemSnapshotJson(item);

        List<Transaction> transactions = transactionRepository.findByBudgetItemId(id);
        if (!transactions.isEmpty()) {
            throw new IllegalStateException(
                    "Cannot delete BudgetItem " + id + " because it has " + transactions.size() + " transaction(s)");
        }

        AuditExecutor.execute(
                auditLogService,
                cz.cvut.fit.budget_app.entity.AuditLog.OperationType.BUDGET_ITEM_DELETED,
                cz.cvut.fit.budget_app.entity.AuditLog.EntityType.BUDGET_ITEM,
                id,
                performedByUserId,
                oldValueJson,
                () -> {
                    budgetItemRepository.delete(item);
                    return null;
                }
        );
    }

    public BudgetItem getOrThrow(Long id) {
        return budgetItemRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("BudgetItem", id));
    }

    @Transactional
    public BudgetItemResponse assignTeam(Long budgetItemId, Long teamId, Long performedByUserId) {
        BudgetItem item = getOrThrow(budgetItemId);
        seasonService.requireOpen(item.getSeason());
        requireNoTransactionsBeforeTeamAssignmentChange(budgetItemId);
        String oldValueJson = AuditSnapshotSerializer.toJson(teamSnapshot(item.getTeam() != null ? item.getTeam().getId() : null));
        Team team = teamService.getOrThrow(teamId);
        item.setTeam(team);
        BudgetItem saved = AuditExecutor.execute(
                auditLogService,
                cz.cvut.fit.budget_app.entity.AuditLog.OperationType.BUDGET_ITEM_TEAM_ASSIGNED,
                cz.cvut.fit.budget_app.entity.AuditLog.EntityType.BUDGET_ITEM,
                performedByUserId,
                oldValueJson,
                () -> budgetItemRepository.save(item),
                BudgetItem::getId,
                i -> AuditSnapshotSerializer.toJson(teamSnapshot(i.getTeam() != null ? i.getTeam().getId() : null))
        );
        return toResponse(saved);
    }

    @Transactional
    public BudgetItemResponse unassignTeam(Long budgetItemId, Long performedByUserId) {
        BudgetItem item = getOrThrow(budgetItemId);
        seasonService.requireOpen(item.getSeason());
        requireNoTransactionsBeforeTeamAssignmentChange(budgetItemId);
        String oldValueJson = AuditSnapshotSerializer.toJson(teamSnapshot(item.getTeam() != null ? item.getTeam().getId() : null));
        item.setTeam(null);
        BudgetItem saved = AuditExecutor.execute(
                auditLogService,
                cz.cvut.fit.budget_app.entity.AuditLog.OperationType.BUDGET_ITEM_TEAM_UNASSIGNED,
                cz.cvut.fit.budget_app.entity.AuditLog.EntityType.BUDGET_ITEM,
                performedByUserId,
                oldValueJson,
                () -> budgetItemRepository.save(item),
                BudgetItem::getId,
                i -> AuditSnapshotSerializer.toJson(teamSnapshot(null))
        );
        return toResponse(saved);
    }

    private String budgetItemSnapshotJson(BudgetItem item) {
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("name", item.getName());
        snapshot.put("type", item.getType());
        snapshot.put("plannedAmount", item.getPlannedAmount());
        snapshot.put("seasonId", item.getSeason() != null ? item.getSeason().getId() : null);
        snapshot.put("fundingSourceId", item.getFundingSource() != null ? item.getFundingSource().getId() : null);
        snapshot.put("teamId", item.getTeam() != null ? item.getTeam().getId() : null);
        return AuditSnapshotSerializer.toJson(snapshot);
    }

    private Map<String, Object> teamSnapshot(Long teamId) {
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("teamId", teamId);
        return snapshot;
    }

    /** Avoids retroactively moving team-scoped visibility of existing transactions (e.g. for team leaders). */
    private void requireNoTransactionsBeforeTeamAssignmentChange(Long budgetItemId) {
        if (transactionRepository.existsByBudgetItem_Id(budgetItemId)) {
            throw new IllegalStateException(
                    "Cannot change team assignment for BudgetItem " + budgetItemId
                            + " because it has one or more transactions");
        }
    }

    private BudgetItemResponse toResponse(BudgetItem item) {
        BigDecimal actual = transactionRepository.findByBudgetItemId(item.getId()).stream()
                .filter(t -> t.getType() == Transaction.TransactionType.ACTUAL
                        && t.getStatus() == Transaction.ApprovalStatus.APPROVED)
                .map(Transaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return BudgetItemResponse.from(item, actual);
    }
}
