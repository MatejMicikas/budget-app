package cz.cvut.fit.budget_app.service;

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
public class ExportService {

    private final TransactionRepository transactionRepository;
    private final BudgetItemRepository budgetItemRepository;

    @Transactional(readOnly = true)
    public String exportTransactionsCsv(Long seasonId) {
        return exportTransactionsCsv(seasonId, null, false);
    }

    @Transactional(readOnly = true)
    public String exportTransactionsCsv(Long seasonId,
                                        Transaction.ApprovalStatus statusFilter,
                                        boolean includeProposed) {
        List<Transaction> transactions = transactionRepository.findBySeasonId(seasonId).stream()
                .filter(t -> {
                    if (statusFilter != null) {
                        return t.getStatus() == statusFilter;
                    }
                    if (includeProposed) {
                        return t.getStatus() == Transaction.ApprovalStatus.APPROVED
                                || t.getStatus() == Transaction.ApprovalStatus.PROPOSED;
                    }
                    return t.getStatus() == Transaction.ApprovalStatus.APPROVED;
                })
                .toList();

        StringBuilder sb = new StringBuilder();
        sb.append("id,date,type,direction,status,amount,budgetItemId,budgetItemName,description\n");
        for (Transaction t : transactions) {
            sb.append(t.getId()).append(',')
                    .append(t.getDate()).append(',')
                    .append(t.getType()).append(',')
                    .append(t.getDirection()).append(',')
                    .append(t.getStatus()).append(',')
                    .append(t.getAmount()).append(',')
                    .append(t.getBudgetItem().getId()).append(',')
                    .append(escapeCsv(t.getBudgetItem().getName())).append(',')
                    .append(escapeCsv(t.getDescription())).append('\n');
        }
        return sb.toString();
    }

    @Transactional(readOnly = true)
    public String exportBudgetItemsCsv(Long seasonId) {
        List<BudgetItem> items = budgetItemRepository.findBySeasonId(seasonId);

        StringBuilder sb = new StringBuilder();
        sb.append("id,name,type,plannedAmount,actualAmount,balance,fundingSourceId,fundingSourceName\n");
        for (BudgetItem item : items) {
            BigDecimal actual = transactionRepository.findByBudgetItemId(item.getId()).stream()
                    .filter(t -> t.getType() == Transaction.TransactionType.ACTUAL
                            && t.getStatus() == Transaction.ApprovalStatus.APPROVED)
                    .map(Transaction::getAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            sb.append(item.getId()).append(',')
                    .append(escapeCsv(item.getName())).append(',')
                    .append(item.getType()).append(',')
                    .append(item.getPlannedAmount()).append(',')
                    .append(actual).append(',')
                    .append(calculateBalance(item, actual)).append(',')
                    .append(item.getFundingSource() != null ? item.getFundingSource().getId() : "").append(',')
                    .append(escapeCsv(item.getFundingSource() != null ? item.getFundingSource().getName() : ""))
                    .append('\n');
        }
        return sb.toString();
    }

    private BigDecimal calculateBalance(BudgetItem item, BigDecimal actualAmount) {
        if (item.getType() == BudgetItem.ItemType.EXPENSE) {
            return item.getPlannedAmount().subtract(actualAmount);
        }
        return actualAmount.subtract(item.getPlannedAmount());
    }

    private String escapeCsv(String value) {
        if (value == null) return "";
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }
}
