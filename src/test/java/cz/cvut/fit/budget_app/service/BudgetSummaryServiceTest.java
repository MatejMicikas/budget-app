package cz.cvut.fit.budget_app.service;

import cz.cvut.fit.budget_app.dto.response.BudgetItemSummaryResponse;
import cz.cvut.fit.budget_app.dto.response.BudgetSummaryResponse;
import cz.cvut.fit.budget_app.entity.BudgetItem;
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
class BudgetSummaryServiceTest {

    @Mock BudgetItemRepository budgetItemRepository;
    @Mock TransactionRepository transactionRepository;

    @InjectMocks BudgetSummaryService budgetSummaryService;

    @Test
    void summary_expenseLine_usesRemainingToSpend_metric() {
        Season season = season(1L);
        BudgetItem expense = item(10L, BudgetItem.ItemType.EXPENSE, season, new BigDecimal("1000"));

        Transaction tx = tx(expense, new BigDecimal("400"), Transaction.TransactionType.ACTUAL);

        when(budgetItemRepository.findBySeasonId(1L)).thenReturn(List.of(expense));
        when(transactionRepository.findByBudgetItemId(10L)).thenReturn(List.of(tx));

        BudgetSummaryResponse summary = budgetSummaryService.getSeasonSummary(1L, null, Transaction.TransactionType.ACTUAL);

        BudgetItemSummaryResponse row = summary.getItems().get(0);
        assertThat(row.getRemainingMetric()).isEqualTo(BudgetItemSummaryResponse.RemainingMetric.REMAINING_TO_SPEND);
        assertThat(row.getRemainingAmount()).isEqualByComparingTo("600");
        assertThat(row.getVarianceAmount()).isEqualByComparingTo("-600");
        assertThat(summary.getTotalRemainingAmount()).isEqualByComparingTo("600");
        assertThat(summary.getTotalVarianceAmount()).isEqualByComparingTo("-600");
    }

    @Test
    void summary_incomeLine_usesIncomeVsPlan_metric() {
        Season season = season(1L);
        BudgetItem income = item(20L, BudgetItem.ItemType.INCOME, season, new BigDecimal("800"));

        Transaction tx = tx(income, new BigDecimal("900"), Transaction.TransactionType.ACTUAL);

        when(budgetItemRepository.findBySeasonId(1L)).thenReturn(List.of(income));
        when(transactionRepository.findByBudgetItemId(20L)).thenReturn(List.of(tx));

        BudgetSummaryResponse summary = budgetSummaryService.getSeasonSummary(1L, null, Transaction.TransactionType.ACTUAL);

        BudgetItemSummaryResponse row = summary.getItems().get(0);
        assertThat(row.getRemainingMetric()).isEqualTo(BudgetItemSummaryResponse.RemainingMetric.INCOME_VS_PLAN);
        assertThat(row.getRemainingAmount()).isEqualByComparingTo("100");
        assertThat(row.getVarianceAmount()).isEqualByComparingTo("100");
        assertThat(summary.getTotalRemainingAmount()).isEqualByComparingTo("0");
        assertThat(summary.getTotalVarianceAmount()).isEqualByComparingTo("100");
    }

    @Test
    void summary_mixedTotals_expenseRemainingOmitsIncome() {
        Season season = season(1L);
        BudgetItem expense = item(10L, BudgetItem.ItemType.EXPENSE, season, new BigDecimal("100"));
        BudgetItem income = item(11L, BudgetItem.ItemType.INCOME, season, new BigDecimal("50"));

        when(budgetItemRepository.findBySeasonId(1L)).thenReturn(List.of(expense, income));
        when(transactionRepository.findByBudgetItemId(10L)).thenReturn(List.of(tx(expense, new BigDecimal("40"), Transaction.TransactionType.ACTUAL)));
        when(transactionRepository.findByBudgetItemId(11L)).thenReturn(List.of(tx(income, new BigDecimal("60"), Transaction.TransactionType.ACTUAL)));

        BudgetSummaryResponse summary = budgetSummaryService.getSeasonSummary(1L, null, Transaction.TransactionType.ACTUAL);

        assertThat(summary.getTotalRemainingAmount()).isEqualByComparingTo("60");
        assertThat(summary.getTotalVarianceAmount()).isEqualByComparingTo("-50");
    }

    private static Season season(Long id) {
        Season s = new Season();
        s.setId(id);
        return s;
    }

    private static BudgetItem item(Long id, BudgetItem.ItemType type, Season season, BigDecimal planned) {
        BudgetItem b = new BudgetItem();
        b.setId(id);
        b.setType(type);
        b.setSeason(season);
        b.setPlannedAmount(planned);
        b.setName("Line");
        return b;
    }

    private static Transaction tx(BudgetItem item, BigDecimal amount, Transaction.TransactionType type) {
        Transaction t = new Transaction();
        t.setAmount(amount);
        t.setType(type);
        t.setBudgetItem(item);
        t.setSeason(item.getSeason());
        t.setStatus(Transaction.ApprovalStatus.APPROVED);
        t.setDirection(item.getType() == BudgetItem.ItemType.INCOME
                ? Transaction.Direction.INCOME
                : Transaction.Direction.EXPENSE);
        t.setDate(LocalDate.of(2025, 1, 15));
        return t;
    }
}
