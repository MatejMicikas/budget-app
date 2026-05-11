package cz.cvut.fit.budget_app.service;

import cz.cvut.fit.budget_app.dto.request.CreateFundingSourceRequest;
import cz.cvut.fit.budget_app.dto.request.UpdateFundingSourceRequest;
import cz.cvut.fit.budget_app.dto.response.FundingSourceResponse;
import cz.cvut.fit.budget_app.dto.response.SeasonFundingSpendingReportResponse;
import cz.cvut.fit.budget_app.entity.FundingSource;
import cz.cvut.fit.budget_app.entity.Season;
import cz.cvut.fit.budget_app.entity.Transaction;
import cz.cvut.fit.budget_app.entity.User;
import cz.cvut.fit.budget_app.exception.ResourceNotFoundException;
import cz.cvut.fit.budget_app.repository.BudgetItemRepository;
import cz.cvut.fit.budget_app.repository.FundingSourceRepository;
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
public class FundingSourceService {
    private record FundingSourceFlowMetrics(
            BigDecimal actualIncome,
            BigDecimal actualExpense,
            BigDecimal plannedIncome,
            BigDecimal plannedExpense
    ) {}

    private final FundingSourceRepository fundingSourceRepository;
    private final BudgetItemRepository budgetItemRepository;
    private final TransactionRepository transactionRepository;
    private final SeasonService seasonService;
    private final AuditLogService auditLogService;

    @Transactional
    public FundingSourceResponse create(CreateFundingSourceRequest request, Long performedByUserId) {
        Season season = seasonService.getOrThrow(request.getSeasonId());
        seasonService.requireOpen(season);

        FundingSource fs = new FundingSource();
        fs.setName(request.getName());
        fs.setType(request.getType());
        fs.setAllocatedAmount(request.getAllocatedAmount());
        fs.setSeason(season);

        FundingSource saved = AuditExecutor.execute(
                auditLogService,
                cz.cvut.fit.budget_app.entity.AuditLog.OperationType.FUNDING_SOURCE_CREATED,
                cz.cvut.fit.budget_app.entity.AuditLog.EntityType.FUNDING_SOURCE,
                performedByUserId,
                null,
                () -> fundingSourceRepository.save(fs),
                FundingSource::getId,
                this::fundingSourceSnapshotJson
        );
        return toResponse(saved);
    }

    @Transactional
    public FundingSourceResponse update(Long id, UpdateFundingSourceRequest request, Long performedByUserId) {
        FundingSource fs = getOrThrow(id);
        seasonService.requireOpen(fs.getSeason());
        String oldValueJson = fundingSourceSnapshotJson(fs);

        BigDecimal actualSpending = calculateActualExpense(id);
        if (request.getAllocatedAmount() != null
                && request.getAllocatedAmount().compareTo(actualSpending) < 0) {
            throw new IllegalArgumentException(
                    "Allocated amount cannot be lower than actual spending (" + actualSpending + ")");
        }

        fs.setName(request.getName());
        fs.setType(request.getType());
        fs.setAllocatedAmount(request.getAllocatedAmount());

        FundingSource saved = AuditExecutor.execute(
                auditLogService,
                cz.cvut.fit.budget_app.entity.AuditLog.OperationType.FUNDING_SOURCE_UPDATED,
                cz.cvut.fit.budget_app.entity.AuditLog.EntityType.FUNDING_SOURCE,
                performedByUserId,
                oldValueJson,
                () -> fundingSourceRepository.save(fs),
                FundingSource::getId,
                this::fundingSourceSnapshotJson
        );
        return toResponse(saved);
    }

    @Transactional
    public void delete(Long id, Long performedByUserId) {
        FundingSource fs = getOrThrow(id);
        seasonService.requireOpen(fs.getSeason());
        String oldValueJson = fundingSourceSnapshotJson(fs);

        if (!budgetItemRepository.findByFundingSourceId(id).isEmpty()) {
            throw new IllegalStateException(
                    "Cannot delete FundingSource " + id + " because it is used by one or more budget items");
        }

        AuditExecutor.execute(
                auditLogService,
                cz.cvut.fit.budget_app.entity.AuditLog.OperationType.FUNDING_SOURCE_DELETED,
                cz.cvut.fit.budget_app.entity.AuditLog.EntityType.FUNDING_SOURCE,
                id,
                performedByUserId,
                oldValueJson,
                () -> {
                    fundingSourceRepository.delete(fs);
                    return null;
                }
        );
    }

    @Transactional(readOnly = true)
    public FundingSourceResponse findById(Long id, User.Role requesterRole, Long requesterTeamId) {
        FundingSource source = getOrThrow(id);
        if (requesterRole == User.Role.TEAM_LEADER) {
            if (requesterTeamId == null) {
                throw new IllegalStateException("TEAM_LEADER must belong to a team");
            }
            if (!budgetItemRepository.existsByFundingSourceIdAndTeamId(id, requesterTeamId)) {
                throw new ResourceNotFoundException("FundingSource", id);
            }
        }
        return toResponse(source);
    }

    @Transactional(readOnly = true)
    public List<FundingSourceResponse> findBySeasonId(Long seasonId, User.Role requesterRole, Long requesterTeamId) {
        List<FundingSource> sources;
        if (requesterRole == User.Role.TEAM_LEADER) {
            if (requesterTeamId == null) {
                throw new IllegalStateException("TEAM_LEADER must belong to a team");
            }
            sources = fundingSourceRepository.findBySeasonIdAndTeamId(seasonId, requesterTeamId);
        } else {
            sources = fundingSourceRepository.findBySeasonId(seasonId);
        }
        return sources.stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public SeasonFundingSpendingReportResponse getSeasonSpendingReport(Long seasonId,
                                                                       User.Role requesterRole,
                                                                       Long requesterTeamId) {
        List<FundingSourceResponse> sources = findBySeasonId(seasonId, requesterRole, requesterTeamId);

        SeasonFundingSpendingReportResponse response = new SeasonFundingSpendingReportResponse();
        response.setSeasonId(seasonId);
        response.setSources(sources);
        response.setSourceCount(sources.size());
        response.setLimitExceededCount((int) sources.stream().filter(FundingSourceResponse::isLimitExceeded).count());
        response.setTotalAllocatedAmount(sources.stream()
                .map(s -> s.getAllocatedAmount() != null ? s.getAllocatedAmount() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add));
        response.setTotalActualIncome(sources.stream()
                .map(FundingSourceResponse::getActualIncome)
                .reduce(BigDecimal.ZERO, BigDecimal::add));
        response.setTotalActualExpense(sources.stream()
                .map(FundingSourceResponse::getActualExpense)
                .reduce(BigDecimal.ZERO, BigDecimal::add));
        response.setTotalPlannedIncome(sources.stream()
                .map(FundingSourceResponse::getPlannedIncome)
                .reduce(BigDecimal.ZERO, BigDecimal::add));
        response.setTotalPlannedExpense(sources.stream()
                .map(FundingSourceResponse::getPlannedExpense)
                .reduce(BigDecimal.ZERO, BigDecimal::add));
        response.setTotalRemainingForExpense(response.getTotalAllocatedAmount().subtract(response.getTotalActualExpense()));
        // Backward-compatible aliases used by current frontend.
        response.setTotalActualSpending(response.getTotalActualExpense());
        response.setTotalRemainingAmount(response.getTotalRemainingForExpense());

        return response;
    }

    public FundingSource getOrThrow(Long id) {
        return fundingSourceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("FundingSource", id));
    }

    private FundingSourceResponse toResponse(FundingSource fs) {
        FundingSourceFlowMetrics metrics = calculateFlowMetrics(fs.getId());
        return FundingSourceResponse.from(
                fs,
                metrics.actualIncome(),
                metrics.actualExpense(),
                metrics.plannedIncome(),
                metrics.plannedExpense()
        );
    }

    BigDecimal calculateActualExpense(Long fundingSourceId) {
        return calculateFlowMetrics(fundingSourceId).actualExpense();
    }

    private FundingSourceFlowMetrics calculateFlowMetrics(Long fundingSourceId) {
        BigDecimal actualIncome = BigDecimal.ZERO;
        BigDecimal actualExpense = BigDecimal.ZERO;
        BigDecimal plannedIncome = BigDecimal.ZERO;
        BigDecimal plannedExpense = BigDecimal.ZERO;

        List<Transaction> transactions = budgetItemRepository.findByFundingSourceId(fundingSourceId).stream()
                .flatMap(item -> transactionRepository.findByBudgetItemId(item.getId()).stream())
                .toList();

        for (Transaction t : transactions) {
            if (t.getStatus() != Transaction.ApprovalStatus.APPROVED) {
                continue;
            }

            if (t.getType() == Transaction.TransactionType.ACTUAL) {
                if (t.getDirection() == Transaction.Direction.INCOME) {
                    actualIncome = actualIncome.add(t.getAmount());
                } else if (t.getDirection() == Transaction.Direction.EXPENSE) {
                    actualExpense = actualExpense.add(t.getAmount());
                }
            } else if (t.getType() == Transaction.TransactionType.PLANNED) {
                if (t.getDirection() == Transaction.Direction.INCOME) {
                    plannedIncome = plannedIncome.add(t.getAmount());
                } else if (t.getDirection() == Transaction.Direction.EXPENSE) {
                    plannedExpense = plannedExpense.add(t.getAmount());
                }
            }
        }

        return new FundingSourceFlowMetrics(actualIncome, actualExpense, plannedIncome, plannedExpense);
    }

    @Transactional(readOnly = true)
    public String getSpendingLimitWarning(Long fundingSourceId) {
        FundingSource fs = getOrThrow(fundingSourceId);
        if (fs.getAllocatedAmount() == null) {
            return null;
        }
        BigDecimal actualExpense = calculateActualExpense(fundingSourceId);
        if (actualExpense.compareTo(fs.getAllocatedAmount()) > 0) {
            return "FundingSource " + fundingSourceId + " limit " + fs.getAllocatedAmount()
                    + " was exceeded by actual expense " + actualExpense;
        }
        return null;
    }

    private String fundingSourceSnapshotJson(FundingSource fs) {
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("name", fs.getName());
        snapshot.put("type", fs.getType());
        snapshot.put("allocatedAmount", fs.getAllocatedAmount());
        snapshot.put("seasonId", fs.getSeason() != null ? fs.getSeason().getId() : null);
        return AuditSnapshotSerializer.toJson(snapshot);
    }
}
