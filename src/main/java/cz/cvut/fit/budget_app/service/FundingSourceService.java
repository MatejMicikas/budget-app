package cz.cvut.fit.budget_app.service;

import cz.cvut.fit.budget_app.dto.request.CreateFundingSourceRequest;
import cz.cvut.fit.budget_app.dto.request.UpdateFundingSourceRequest;
import cz.cvut.fit.budget_app.dto.response.FundingSourceResponse;
import cz.cvut.fit.budget_app.entity.FundingSource;
import cz.cvut.fit.budget_app.entity.Season;
import cz.cvut.fit.budget_app.entity.Transaction;
import cz.cvut.fit.budget_app.exception.ResourceNotFoundException;
import cz.cvut.fit.budget_app.repository.BudgetItemRepository;
import cz.cvut.fit.budget_app.repository.FundingSourceRepository;
import cz.cvut.fit.budget_app.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class FundingSourceService {

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

        FundingSource saved = fundingSourceRepository.save(fs);
        auditLogService.logFundingSourceCreated(saved.getId(), performedByUserId);
        return toResponse(saved);
    }

    @Transactional
    public FundingSourceResponse update(Long id, UpdateFundingSourceRequest request, Long performedByUserId) {
        FundingSource fs = getOrThrow(id);
        seasonService.requireOpen(fs.getSeason());

        fs.setName(request.getName());
        fs.setType(request.getType());
        fs.setAllocatedAmount(request.getAllocatedAmount());

        FundingSource saved = fundingSourceRepository.save(fs);
        auditLogService.logFundingSourceUpdated(saved.getId(), performedByUserId);
        return toResponse(saved);
    }

    @Transactional
    public void delete(Long id, Long performedByUserId) {
        FundingSource fs = getOrThrow(id);
        seasonService.requireOpen(fs.getSeason());

        if (!budgetItemRepository.findByFundingSourceId(id).isEmpty()) {
            throw new IllegalStateException(
                    "Cannot delete FundingSource " + id + " because it is used by one or more budget items");
        }

        fundingSourceRepository.delete(fs);
        auditLogService.logFundingSourceDeleted(id, performedByUserId);
    }

    @Transactional(readOnly = true)
    public FundingSourceResponse findById(Long id) {
        return toResponse(getOrThrow(id));
    }

    @Transactional(readOnly = true)
    public List<FundingSourceResponse> findBySeasonId(Long seasonId) {
        return fundingSourceRepository.findBySeasonId(seasonId).stream()
                .map(this::toResponse)
                .toList();
    }

    public FundingSource getOrThrow(Long id) {
        return fundingSourceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("FundingSource", id));
    }

    private FundingSourceResponse toResponse(FundingSource fs) {
        BigDecimal actual = calculateActualSpending(fs.getId());
        return FundingSourceResponse.from(fs, actual);
    }

    BigDecimal calculateActualSpending(Long fundingSourceId) {
        return budgetItemRepository.findByFundingSourceId(fundingSourceId).stream()
                .flatMap(item -> transactionRepository.findByBudgetItemId(item.getId()).stream())
                .filter(t -> t.getType() == Transaction.TransactionType.ACTUAL
                        && t.getDirection() == Transaction.Direction.EXPENSE)
                .map(Transaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    @Transactional(readOnly = true)
    public void validateSpendingLimitOrThrow(Long fundingSourceId) {
        FundingSource fs = getOrThrow(fundingSourceId);
        if (fs.getAllocatedAmount() == null) {
            return;
        }
        BigDecimal actual = calculateActualSpending(fundingSourceId);
        if (actual.compareTo(fs.getAllocatedAmount()) > 0) {
            throw new cz.cvut.fit.budget_app.exception.FundingLimitExceededException(
                    fundingSourceId, fs.getAllocatedAmount(), actual);
        }
    }
}
