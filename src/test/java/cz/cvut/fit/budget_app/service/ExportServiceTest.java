package cz.cvut.fit.budget_app.service;

import cz.cvut.fit.budget_app.entity.BudgetItem;
import cz.cvut.fit.budget_app.entity.FundingSource;
import cz.cvut.fit.budget_app.entity.Season;
import cz.cvut.fit.budget_app.entity.Transaction;
import cz.cvut.fit.budget_app.repository.BudgetItemRepository;
import cz.cvut.fit.budget_app.repository.TransactionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ExportServiceTest {

    @Mock TransactionRepository transactionRepository;
    @Mock BudgetItemRepository budgetItemRepository;

    @InjectMocks ExportService exportService;

    @Test
    void exportTransactionsCsv_defaultExportsApprovedOnly_andEscapesCommasQuotesAndNewlines() {
        Season season = season();
        BudgetItem item = budgetItem(10L, "Hall, \"A\"", season);
        Transaction tx = transaction(1L, item, "Line1\n\"quoted\",text");
        Transaction proposed = transaction(2L, item, "should not be exported");
        proposed.setStatus(Transaction.ApprovalStatus.PROPOSED);
        when(transactionRepository.findBySeasonId(1L)).thenReturn(List.of(tx, proposed));

        String csv = exportService.exportTransactionsCsv(1L);

        assertThat(csv).contains("status");
        assertThat(csv).contains("APPROVED");
        assertThat(csv).doesNotContain("PROPOSED");
        assertThat(csv).contains("\"Hall, \"\"A\"\"\"");
        assertThat(csv).contains("\"Line1\n\"\"quoted\"\",text\"");
    }

    @Test
    void exportTransactionsCsv_includeProposed_exportsApprovedAndProposed() {
        Season season = season();
        BudgetItem item = budgetItem(10L, "Item", season);
        Transaction approved = transaction(1L, item, "approved");
        approved.setStatus(Transaction.ApprovalStatus.APPROVED);
        Transaction proposed = transaction(2L, item, "proposed");
        proposed.setStatus(Transaction.ApprovalStatus.PROPOSED);
        when(transactionRepository.findBySeasonId(1L)).thenReturn(List.of(approved, proposed));

        String csv = exportService.exportTransactionsCsv(1L, null, true);

        assertThat(csv).contains("APPROVED");
        assertThat(csv).contains("PROPOSED");
    }

    @Test
    void exportTransactionsCsv_statusFilter_exportsOnlyRequestedStatus() {
        Season season = season();
        BudgetItem item = budgetItem(10L, "Item", season);
        Transaction approved = transaction(1L, item, "approved");
        approved.setStatus(Transaction.ApprovalStatus.APPROVED);
        Transaction rejected = transaction(2L, item, "rejected");
        rejected.setStatus(Transaction.ApprovalStatus.REJECTED);
        when(transactionRepository.findBySeasonId(1L)).thenReturn(List.of(approved, rejected));

        String csv = exportService.exportTransactionsCsv(1L, Transaction.ApprovalStatus.REJECTED, false);

        assertThat(csv).contains("REJECTED");
        assertThat(csv).doesNotContain("APPROVED");
    }

    @Test
    void exportBudgetItemsCsv_countsOnlyApprovedActualTransactions() {
        Season season = season();
        BudgetItem item = budgetItem(10L, "Item", season);
        Transaction approvedActual = transaction(1L, item, "ok");
        approvedActual.setType(Transaction.TransactionType.ACTUAL);
        approvedActual.setStatus(Transaction.ApprovalStatus.APPROVED);
        approvedActual.setAmount(new BigDecimal("100.00"));

        Transaction proposedActual = transaction(2L, item, "not approved");
        proposedActual.setType(Transaction.TransactionType.ACTUAL);
        proposedActual.setStatus(Transaction.ApprovalStatus.PROPOSED);
        proposedActual.setAmount(new BigDecimal("900.00"));

        when(budgetItemRepository.findBySeasonId(1L)).thenReturn(List.of(item));
        when(transactionRepository.findByBudgetItemId(10L)).thenReturn(List.of(approvedActual, proposedActual));

        String csv = exportService.exportBudgetItemsCsv(1L);

        assertThat(csv).contains(",100.00,");
        assertThat(csv).doesNotContain(",1000.00,");
    }

    private Season season() {
        Season s = new Season();
        s.setId(1L);
        s.setName("2024/2025");
        s.setDateFrom(LocalDate.of(2024, 9, 1));
        s.setDateTo(LocalDate.of(2025, 6, 30));
        s.setStatus(Season.SeasonStatus.OPEN);
        return s;
    }

    private BudgetItem budgetItem(Long id, String name, Season season) {
        BudgetItem item = new BudgetItem();
        item.setId(id);
        item.setName(name);
        item.setType(BudgetItem.ItemType.EXPENSE);
        item.setPlannedAmount(new BigDecimal("500.00"));
        item.setSeason(season);
        FundingSource fs = new FundingSource();
        fs.setId(20L);
        fs.setName("Grant");
        item.setFundingSource(fs);
        return item;
    }

    private Transaction transaction(Long id, BudgetItem item, String description) {
        Transaction tx = new Transaction();
        tx.setId(id);
        tx.setDate(LocalDate.of(2024, 10, 1));
        tx.setType(Transaction.TransactionType.ACTUAL);
        tx.setDirection(Transaction.Direction.EXPENSE);
        tx.setAmount(new BigDecimal("50.00"));
        tx.setDescription(description);
        tx.setBudgetItem(item);
        tx.setSeason(item.getSeason());
        tx.setStatus(Transaction.ApprovalStatus.APPROVED);
        return tx;
    }
}
