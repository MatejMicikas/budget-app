package cz.cvut.fit.budget_app.service;

import cz.cvut.fit.budget_app.dto.request.CreateBudgetItemRequest;
import cz.cvut.fit.budget_app.dto.request.UpdateBudgetItemRequest;
import cz.cvut.fit.budget_app.dto.response.BudgetItemResponse;
import cz.cvut.fit.budget_app.entity.BudgetItem;
import cz.cvut.fit.budget_app.entity.FundingSource;
import cz.cvut.fit.budget_app.entity.Season;
import cz.cvut.fit.budget_app.entity.Transaction;
import cz.cvut.fit.budget_app.exception.ResourceNotFoundException;
import cz.cvut.fit.budget_app.repository.BudgetItemRepository;
import cz.cvut.fit.budget_app.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class BudgetItemService {

    private final BudgetItemRepository budgetItemRepository;
    private final TransactionRepository transactionRepository;
    private final SeasonService seasonService;
    private final FundingSourceService fundingSourceService;
    private final AuditLogService auditLogService;

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

        BudgetItem saved = budgetItemRepository.save(item);
        auditLogService.logBudgetItemCreated(saved.getId(), performedByUserId);

        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public BudgetItemResponse findById(Long id) {
        return toResponse(getOrThrow(id));
    }

    @Transactional(readOnly = true)
    public List<BudgetItemResponse> findBySeasonId(Long seasonId) {
        return budgetItemRepository.findBySeasonId(seasonId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public BudgetItemResponse update(Long id, UpdateBudgetItemRequest request) {
        BudgetItem item = getOrThrow(id);
        seasonService.requireOpen(item.getSeason());

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

        return toResponse(budgetItemRepository.save(item));
    }

    @Transactional
    public void delete(Long id, Long performedByUserId) {
        BudgetItem item = getOrThrow(id);
        seasonService.requireOpen(item.getSeason());

        List<Transaction> transactions = transactionRepository.findByBudgetItemId(id);
        if (!transactions.isEmpty()) {
            throw new IllegalStateException(
                    "Cannot delete BudgetItem " + id + " because it has " + transactions.size() + " transaction(s)");
        }

        budgetItemRepository.delete(item);
        auditLogService.logBudgetItemDeleted(id, performedByUserId);
    }

    public BudgetItem getOrThrow(Long id) {
        return budgetItemRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("BudgetItem", id));
    }

    private BudgetItemResponse toResponse(BudgetItem item) {
        BigDecimal actual = transactionRepository.findByBudgetItemId(item.getId()).stream()
                .filter(t -> t.getType() == Transaction.TransactionType.ACTUAL)
                .map(Transaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return BudgetItemResponse.from(item, actual);
    }
}
