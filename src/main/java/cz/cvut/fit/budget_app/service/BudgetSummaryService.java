package cz.cvut.fit.budget_app.service;

import cz.cvut.fit.budget_app.dto.response.BudgetItemSummaryResponse;
import cz.cvut.fit.budget_app.dto.response.BudgetItemSummaryResponse.RemainingMetric;
import cz.cvut.fit.budget_app.dto.response.BudgetSummaryResponse;
import cz.cvut.fit.budget_app.entity.BudgetItem;
import cz.cvut.fit.budget_app.entity.Transaction;
import cz.cvut.fit.budget_app.repository.BudgetItemRepository;
import cz.cvut.fit.budget_app.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class BudgetSummaryService {

    private final BudgetItemRepository budgetItemRepository;
    private final TransactionRepository transactionRepository;

    @Transactional(readOnly = true)
    public BudgetSummaryResponse getSeasonSummary(Long seasonId,
                                                  BudgetItem.ItemType itemType,
                                                  Transaction.TransactionType transactionType) {
        return buildSummaryResponse(budgetItemRepository.findBySeasonId(seasonId), seasonId, itemType, transactionType);
    }

    @Transactional(readOnly = true)
    public BudgetSummaryResponse getSeasonSummaryForTeam(Long seasonId,
                                                         Long teamId,
                                                         BudgetItem.ItemType itemType,
                                                         Transaction.TransactionType transactionType) {
        return buildSummaryResponse(
                budgetItemRepository.findBySeasonIdAndTeamId(seasonId, teamId),
                seasonId,
                itemType,
                transactionType);
    }

    private BudgetSummaryResponse buildSummaryResponse(List<BudgetItem> seasonItems,
                                                       Long seasonId,
                                                       BudgetItem.ItemType itemType,
                                                       Transaction.TransactionType transactionType) {
        Transaction.TransactionType usedType = transactionType != null
                ? transactionType
                : Transaction.TransactionType.ACTUAL;

        List<BudgetItemSummaryResponse> itemSummaries = seasonItems.stream()
                .filter(item -> itemType == null || item.getType() == itemType)
                .map(item -> toItemSummary(item, usedType))
                .toList();

        BudgetSummaryResponse response = new BudgetSummaryResponse();
        response.setSeasonId(seasonId);
        response.setItemTypeFilter(itemType);
        response.setTransactionTypeUsed(usedType);
        response.setItemCount(itemSummaries.size());
        response.setItems(itemSummaries);
        response.setTotalPlannedAmount(sum(itemSummaries, BudgetItemSummaryResponse::getPlannedAmount));
        response.setTotalTransactionAmount(sum(itemSummaries, BudgetItemSummaryResponse::getTransactionAmount));
        response.setTotalRemainingAmount(sumExpenseRemaining(itemSummaries));
        response.setTotalVarianceAmount(sum(itemSummaries, BudgetItemSummaryResponse::getVarianceAmount));
        return response;
    }

    private BudgetItemSummaryResponse toItemSummary(BudgetItem item, Transaction.TransactionType transactionType) {
        BigDecimal transactionAmount = transactionRepository.findByBudgetItemId(item.getId()).stream()
                .filter(t -> t.getType() == transactionType)
                .filter(t -> t.getStatus() == Transaction.ApprovalStatus.APPROVED)
                .map(Transaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal plannedAmount = item.getPlannedAmount();
        BigDecimal varianceAmount = transactionAmount.subtract(plannedAmount);

        BudgetItemSummaryResponse response = new BudgetItemSummaryResponse();
        response.setBudgetItemId(item.getId());
        response.setBudgetItemName(item.getName());
        response.setItemType(item.getType());
        response.setPlannedAmount(plannedAmount);
        response.setTransactionAmount(transactionAmount);
        response.setVarianceAmount(varianceAmount);

        if (item.getType() == BudgetItem.ItemType.EXPENSE) {
            response.setRemainingMetric(RemainingMetric.REMAINING_TO_SPEND);
            response.setRemainingAmount(plannedAmount.subtract(transactionAmount));
        } else {
            response.setRemainingMetric(RemainingMetric.INCOME_VS_PLAN);
            response.setRemainingAmount(transactionAmount.subtract(plannedAmount));
        }
        return response;
    }

    private BigDecimal sumExpenseRemaining(List<BudgetItemSummaryResponse> items) {
        return items.stream()
                .filter(i -> i.getItemType() == BudgetItem.ItemType.EXPENSE)
                .map(BudgetItemSummaryResponse::getRemainingAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal sum(List<BudgetItemSummaryResponse> items,
                           java.util.function.Function<BudgetItemSummaryResponse, BigDecimal> extractor) {
        return items.stream()
                .map(extractor)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
