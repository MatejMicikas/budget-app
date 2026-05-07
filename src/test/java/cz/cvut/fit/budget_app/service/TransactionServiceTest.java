package cz.cvut.fit.budget_app.service;

import cz.cvut.fit.budget_app.dto.request.CreateTransactionRequest;
import cz.cvut.fit.budget_app.dto.response.TransactionResponse;
import cz.cvut.fit.budget_app.entity.*;
import cz.cvut.fit.budget_app.exception.ResourceNotFoundException;
import cz.cvut.fit.budget_app.exception.SeasonClosedException;
import cz.cvut.fit.budget_app.repository.TransactionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {

    @Mock TransactionRepository transactionRepository;
    @Mock BudgetItemService budgetItemService;
    @Mock SeasonService seasonService;
    @Mock AuditLogService auditLogService;

    @InjectMocks TransactionService transactionService;

    @Test
    void create_validExpenseTransaction_success() {
        Season season = openSeason(1L);
        BudgetItem item = buildItem(10L, BudgetItem.ItemType.EXPENSE, season);
        when(budgetItemService.getOrThrow(10L)).thenReturn(item);
        doNothing().when(seasonService).requireOpen(season);
        when(transactionRepository.save(any())).thenAnswer(inv -> {
            Transaction t = inv.getArgument(0);
            t.setId(100L);
            return t;
        });

        CreateTransactionRequest req = buildRequest(Transaction.TransactionType.ACTUAL,
                Transaction.Direction.EXPENSE, 10L);
        TransactionResponse result = transactionService.create(req, 99L);

        assertThat(result.getAmount()).isEqualByComparingTo(new BigDecimal("200.00"));
        assertThat(result.getType()).isEqualTo(Transaction.TransactionType.ACTUAL);
        verify(auditLogService).logTransactionCreated(100L, 99L);
    }

    @Test
    void create_directionMismatchWithBudgetItemType_throwsIllegalArgument() {
        Season season = openSeason(1L);
        // BudgetItem je EXPENSE, ale posíláme INCOME transakci
        BudgetItem item = buildItem(10L, BudgetItem.ItemType.EXPENSE, season);
        when(budgetItemService.getOrThrow(10L)).thenReturn(item);
        doNothing().when(seasonService).requireOpen(season);

        CreateTransactionRequest req = buildRequest(Transaction.TransactionType.ACTUAL,
                Transaction.Direction.INCOME, 10L);

        assertThatThrownBy(() -> transactionService.create(req, 99L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("direction");
    }

    @Test
    void create_closedSeason_throwsSeasonClosedException() {
        Season season = openSeason(1L);
        BudgetItem item = buildItem(10L, BudgetItem.ItemType.EXPENSE, season);
        when(budgetItemService.getOrThrow(10L)).thenReturn(item);
        doThrow(new SeasonClosedException(1L)).when(seasonService).requireOpen(season);

        CreateTransactionRequest req = buildRequest(Transaction.TransactionType.ACTUAL,
                Transaction.Direction.EXPENSE, 10L);

        assertThatThrownBy(() -> transactionService.create(req, 99L))
                .isInstanceOf(SeasonClosedException.class);
    }

    @Test
    void create_seasonMismatch_throwsIllegalArgumentException() {
        Season season = openSeason(1L);
        BudgetItem item = buildItem(10L, BudgetItem.ItemType.EXPENSE, season);
        when(budgetItemService.getOrThrow(10L)).thenReturn(item);

        CreateTransactionRequest req = buildRequest(Transaction.TransactionType.ACTUAL,
                Transaction.Direction.EXPENSE, 10L);
        req.setSeasonId(2L);

        assertThatThrownBy(() -> transactionService.create(req, 99L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("seasonId");

        verify(seasonService, never()).requireOpen(any());
    }

    @Test
    void delete_closedSeason_throwsSeasonClosedException() {
        Season season = openSeason(1L);
        Transaction tx = buildTransaction(50L, season);
        when(transactionRepository.findById(50L)).thenReturn(Optional.of(tx));
        doThrow(new SeasonClosedException(1L)).when(seasonService).requireOpen(season);

        assertThatThrownBy(() -> transactionService.delete(50L, 99L))
                .isInstanceOf(SeasonClosedException.class);

        verify(transactionRepository, never()).delete(any());
    }

    @Test
    void delete_openSeason_deletesAndLogsAudit() {
        Season season = openSeason(1L);
        Transaction tx = buildTransaction(50L, season);
        when(transactionRepository.findById(50L)).thenReturn(Optional.of(tx));
        doNothing().when(seasonService).requireOpen(season);

        transactionService.delete(50L, 99L);

        verify(transactionRepository).delete(tx);
        verify(auditLogService).logTransactionDeleted(50L, 99L);
    }

    @Test
    void findById_notFound_throwsResourceNotFoundException() {
        when(transactionRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> transactionService.findById(999L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ---- helpers ----

    private Season openSeason(Long id) {
        Season s = new Season();
        s.setId(id);
        s.setName("Season");
        s.setDateFrom(LocalDate.of(2024, 9, 1));
        s.setDateTo(LocalDate.of(2025, 6, 30));
        s.setStatus(Season.SeasonStatus.OPEN);
        return s;
    }

    private BudgetItem buildItem(Long id, BudgetItem.ItemType type, Season season) {
        BudgetItem b = new BudgetItem();
        b.setId(id);
        b.setName("Item");
        b.setType(type);
        b.setPlannedAmount(new BigDecimal("1000.00"));
        b.setSeason(season);
        return b;
    }

    private Transaction buildTransaction(Long id, Season season) {
        BudgetItem item = buildItem(10L, BudgetItem.ItemType.EXPENSE, season);
        Transaction t = new Transaction();
        t.setId(id);
        t.setDate(LocalDate.now());
        t.setAmount(new BigDecimal("200.00"));
        t.setType(Transaction.TransactionType.ACTUAL);
        t.setDirection(Transaction.Direction.EXPENSE);
        t.setBudgetItem(item);
        t.setSeason(season);
        return t;
    }

    private CreateTransactionRequest buildRequest(Transaction.TransactionType type,
                                                   Transaction.Direction direction,
                                                   Long budgetItemId) {
        CreateTransactionRequest r = new CreateTransactionRequest();
        r.setDate(LocalDate.now());
        r.setAmount(new BigDecimal("200.00"));
        r.setType(type);
        r.setDirection(direction);
        r.setSeasonId(1L);
        r.setBudgetItemId(budgetItemId);
        return r;
    }
}
