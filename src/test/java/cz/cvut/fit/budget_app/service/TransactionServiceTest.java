package cz.cvut.fit.budget_app.service;

import cz.cvut.fit.budget_app.dto.request.CancelTransactionRequest;
import cz.cvut.fit.budget_app.dto.request.CreateTransactionRequest;
import cz.cvut.fit.budget_app.dto.request.RealizePlannedTransactionRequest;
import cz.cvut.fit.budget_app.dto.request.UpdateTransactionRequest;
import cz.cvut.fit.budget_app.dto.response.TransactionResponse;
import cz.cvut.fit.budget_app.entity.*;
import cz.cvut.fit.budget_app.exception.ResourceNotFoundException;
import cz.cvut.fit.budget_app.exception.SeasonClosedException;
import cz.cvut.fit.budget_app.repository.TransactionRepository;
import cz.cvut.fit.budget_app.repository.UserRepository;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {

    @Mock TransactionRepository transactionRepository;
    @Mock BudgetItemService budgetItemService;
    @Mock SeasonService seasonService;
    @Mock FundingSourceService fundingSourceService;
    @Mock AuditLogService auditLogService;
    @Mock UserRepository userRepository;

    @InjectMocks TransactionService transactionService;

    @Test
    void create_validExpenseTransaction_success() {
        Season season = openSeason(1L);
        BudgetItem item = buildItem(10L, BudgetItem.ItemType.EXPENSE, season);
        when(budgetItemService.getOrThrow(10L)).thenReturn(item);
        when(userRepository.findById(99L)).thenReturn(Optional.of(buildUser(99L, User.Role.TREASURER)));
        doNothing().when(seasonService).requireOpen(season);
        when(transactionRepository.save(any())).thenAnswer(inv -> {
            Transaction t = inv.getArgument(0);
            t.setId(100L);
            return t;
        });

        CreateTransactionRequest req = buildRequest(Transaction.TransactionType.ACTUAL,
                Transaction.Direction.EXPENSE, 10L);
        TransactionResponse result = transactionService.create(req, 99L, User.Role.TREASURER, null);

        assertThat(result.getAmount()).isEqualByComparingTo(new BigDecimal("200.00"));
        assertThat(result.getType()).isEqualTo(Transaction.TransactionType.ACTUAL);
        verify(auditLogService).log(eq(AuditLog.OperationType.TRANSACTION_CREATED), eq(AuditLog.EntityType.TRANSACTION), eq(100L), eq(99L), eq(null), any());
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

        assertThatThrownBy(() -> transactionService.create(req, 99L, User.Role.TREASURER, null))
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

        assertThatThrownBy(() -> transactionService.create(req, 99L, User.Role.TREASURER, null))
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

        assertThatThrownBy(() -> transactionService.create(req, 99L, User.Role.TREASURER, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("seasonId");

        verify(seasonService, never()).requireOpen(any());
    }

    @Test
    void create_dateOutsideSeason_throwsIllegalArgumentException() {
        Season season = openSeason(1L);
        BudgetItem item = buildItem(10L, BudgetItem.ItemType.EXPENSE, season);
        when(budgetItemService.getOrThrow(10L)).thenReturn(item);
        doNothing().when(seasonService).requireOpen(season);

        CreateTransactionRequest req = buildRequest(Transaction.TransactionType.ACTUAL,
                Transaction.Direction.EXPENSE, 10L);
        req.setDate(LocalDate.of(2026, 1, 1));

        assertThatThrownBy(() -> transactionService.create(req, 99L, User.Role.TREASURER, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("within season range");
    }

    @Test
    void cancel_closedSeason_throwsSeasonClosedException() {
        Season season = openSeason(1L);
        Transaction tx = buildTransaction(50L, season);
        when(transactionRepository.findById(50L)).thenReturn(Optional.of(tx));
        doThrow(new SeasonClosedException(1L)).when(seasonService).requireOpen(season);

        assertThatThrownBy(() -> transactionService.cancel(50L, new CancelTransactionRequest(), 99L))
                .isInstanceOf(SeasonClosedException.class);

        verify(transactionRepository, never()).save(any());
    }

    @Test
    void update_openSeason_updatesTransaction() {
        Season season = openSeason(1L);
        Transaction tx = buildTransaction(50L, season);

        when(transactionRepository.findById(50L)).thenReturn(Optional.of(tx));
        when(transactionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        doNothing().when(seasonService).requireOpen(season);

        UpdateTransactionRequest req = buildUpdateRequest();
        req.setAmount(new BigDecimal("300.00"));

        TransactionResponse result = transactionService.update(50L, req, 99L);

        assertThat(result.getAmount()).isEqualByComparingTo("300.00");
        verify(transactionRepository).save(tx);
    }

    @Test
    void update_closedSeason_throwsSeasonClosedException() {
        Season season = openSeason(1L);
        Transaction tx = buildTransaction(50L, season);
        when(transactionRepository.findById(50L)).thenReturn(Optional.of(tx));
        doThrow(new SeasonClosedException(1L)).when(seasonService).requireOpen(season);

        UpdateTransactionRequest req = buildUpdateRequest();

        assertThatThrownBy(() -> transactionService.update(50L, req, 99L))
                .isInstanceOf(SeasonClosedException.class);
    }

    @Test
    void update_dateOutsideSeason_throwsIllegalArgumentException() {
        Season season = openSeason(1L);
        Transaction tx = buildTransaction(50L, season);
        when(transactionRepository.findById(50L)).thenReturn(Optional.of(tx));
        doNothing().when(seasonService).requireOpen(season);

        UpdateTransactionRequest req = buildUpdateRequest();
        req.setDate(LocalDate.of(2026, 1, 1));

        assertThatThrownBy(() -> transactionService.update(50L, req, 99L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("within season range");
    }

    @Test
    void update_rejectedTransaction_throwsIllegalStateException() {
        Season season = openSeason(1L);
        Transaction tx = buildTransaction(50L, season);
        tx.setStatus(Transaction.ApprovalStatus.REJECTED);
        when(transactionRepository.findById(50L)).thenReturn(Optional.of(tx));
        doNothing().when(seasonService).requireOpen(season);

        UpdateTransactionRequest req = buildUpdateRequest();

        assertThatThrownBy(() -> transactionService.update(50L, req, 99L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("current status");
    }

    @Test
    void update_approvedCannotReassignBudgetItem_throws() {
        Season season = openSeason(1L);
        BudgetItem item10 = buildItem(10L, BudgetItem.ItemType.EXPENSE, season);
        Transaction tx = buildTransaction(50L, season);
        tx.setBudgetItem(item10);
        tx.setStatus(Transaction.ApprovalStatus.APPROVED);
        when(transactionRepository.findById(50L)).thenReturn(Optional.of(tx));
        doNothing().when(seasonService).requireOpen(season);

        UpdateTransactionRequest req = buildUpdateRequest();
        req.setBudgetItemId(11L);

        assertThatThrownBy(() -> transactionService.update(50L, req, 99L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("approved");
    }

    @Test
    void update_proposedCanReassignBudgetItemWithinSeason_updatesLink() {
        Season season = openSeason(1L);
        BudgetItem item10 = buildItem(10L, BudgetItem.ItemType.EXPENSE, season);
        BudgetItem item11 = buildItem(11L, BudgetItem.ItemType.EXPENSE, season);
        Transaction tx = buildTransaction(50L, season);
        tx.setType(Transaction.TransactionType.PLANNED);
        tx.setStatus(Transaction.ApprovalStatus.PROPOSED);
        tx.setBudgetItem(item10);

        when(transactionRepository.findById(50L)).thenReturn(Optional.of(tx));
        when(budgetItemService.getOrThrow(11L)).thenReturn(item11);
        when(transactionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        doNothing().when(seasonService).requireOpen(any(Season.class));

        UpdateTransactionRequest req = buildUpdateRequest();
        req.setBudgetItemId(11L);
        req.setAmount(new BigDecimal("150.00"));

        TransactionResponse result = transactionService.update(50L, req, 99L);

        assertThat(result.getBudgetItemId()).isEqualTo(11L);
        assertThat(result.getAmount()).isEqualByComparingTo("150.00");
        verify(transactionRepository).save(tx);
    }

    @Test
    void update_doesNotChangePlannedVersusActualType() {
        Season season = openSeason(1L);
        BudgetItem item = buildItem(10L, BudgetItem.ItemType.EXPENSE, season);
        Transaction tx = buildTransaction(50L, season);
        tx.setType(Transaction.TransactionType.PLANNED);
        tx.setStatus(Transaction.ApprovalStatus.PROPOSED);
        when(transactionRepository.findById(50L)).thenReturn(Optional.of(tx));
        when(transactionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        doNothing().when(seasonService).requireOpen(season);

        UpdateTransactionRequest req = buildUpdateRequest();
        req.setAmount(new BigDecimal("999.00"));

        TransactionResponse result = transactionService.update(50L, req, 99L);

        assertThat(result.getType()).isEqualTo(Transaction.TransactionType.PLANNED);
    }

    @Test
    void create_asTeamLeader_setsProposedStatus() {
        Season season = openSeason(1L);
        BudgetItem item = buildItem(10L, BudgetItem.ItemType.EXPENSE, season);
        Team team = new Team();
        team.setId(5L);
        item.setTeam(team);
        when(budgetItemService.getOrThrow(10L)).thenReturn(item);
        when(userRepository.findById(99L)).thenReturn(Optional.of(buildUser(99L, User.Role.TEAM_LEADER)));
        doNothing().when(seasonService).requireOpen(season);
        when(transactionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        CreateTransactionRequest req = buildRequest(Transaction.TransactionType.PLANNED,
                Transaction.Direction.EXPENSE, 10L);
        TransactionResponse result = transactionService.create(req, 99L, User.Role.TEAM_LEADER, 5L);

        assertThat(result.getStatus()).isEqualTo(Transaction.ApprovalStatus.PROPOSED);
    }

    @Test
    void create_whenFundingLimitExceeded_returnsWarningButDoesNotFail() {
        Season season = openSeason(1L);
        FundingSource fundingSource = new FundingSource();
        fundingSource.setId(7L);
        BudgetItem item = buildItem(10L, BudgetItem.ItemType.EXPENSE, season);
        item.setFundingSource(fundingSource);
        when(budgetItemService.getOrThrow(10L)).thenReturn(item);
        when(userRepository.findById(99L)).thenReturn(Optional.of(buildUser(99L, User.Role.TREASURER)));
        doNothing().when(seasonService).requireOpen(season);
        when(fundingSourceService.getSpendingLimitWarning(7L)).thenReturn("Limit exceeded");
        when(transactionRepository.save(any())).thenAnswer(inv -> {
            Transaction t = inv.getArgument(0);
            t.setId(100L);
            return t;
        });

        CreateTransactionRequest req = buildRequest(Transaction.TransactionType.ACTUAL,
                Transaction.Direction.EXPENSE, 10L);
        TransactionResponse result = transactionService.create(req, 99L, User.Role.TREASURER, null);

        assertThat(result.isFundingLimitExceededWarning()).isTrue();
        assertThat(result.getFundingLimitWarningMessage()).isEqualTo("Limit exceeded");
        verify(transactionRepository).save(any(Transaction.class));
    }

    @Test
    void cancel_openSeason_softCancelsAndLogsAudit() {
        Season season = openSeason(1L);
        Transaction tx = buildTransaction(50L, season);
        when(transactionRepository.findById(50L)).thenReturn(Optional.of(tx));
        doNothing().when(seasonService).requireOpen(season);
        when(userRepository.findById(99L)).thenReturn(Optional.of(buildUser(99L, User.Role.TREASURER)));
        when(transactionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        CancelTransactionRequest req = new CancelTransactionRequest();
        req.setReason("Duplicate entry");

        TransactionResponse result = transactionService.cancel(50L, req, 99L);

        assertThat(result.getStatus()).isEqualTo(Transaction.ApprovalStatus.CANCELLED);
        verify(transactionRepository).save(tx);
        verify(auditLogService).log(eq(AuditLog.OperationType.TRANSACTION_CANCELLED), eq(AuditLog.EntityType.TRANSACTION), eq(50L), eq(99L), any(), any());
        verify(transactionRepository, never()).delete(any());
    }

    @Test
    void cancel_rejectedTransaction_throwsIllegalStateException() {
        Season season = openSeason(1L);
        Transaction tx = buildTransaction(50L, season);
        tx.setStatus(Transaction.ApprovalStatus.REJECTED);
        when(transactionRepository.findById(50L)).thenReturn(Optional.of(tx));
        doNothing().when(seasonService).requireOpen(season);

        assertThatThrownBy(() -> transactionService.cancel(50L, new CancelTransactionRequest(), 99L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Rejected");
    }

    @Test
    void cancel_approvedPlannedWithRealizedActualThrows() {
        Season season = openSeason(1L);
        Transaction tx = buildTransaction(50L, season);
        tx.setType(Transaction.TransactionType.PLANNED);
        tx.setStatus(Transaction.ApprovalStatus.APPROVED);
        when(transactionRepository.findById(50L)).thenReturn(Optional.of(tx));
        when(transactionRepository.existsByPlannedTransaction_Id(50L)).thenReturn(true);
        doNothing().when(seasonService).requireOpen(season);

        assertThatThrownBy(() -> transactionService.cancel(50L, new CancelTransactionRequest(), 99L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("realized actual");
    }

    @Test
    void delete_openSeason_deletesAndLogsTransactionDeleted() {
        Season season = openSeason(1L);
        Transaction tx = buildTransaction(50L, season);
        when(transactionRepository.findById(50L)).thenReturn(Optional.of(tx));
        doNothing().when(seasonService).requireOpen(season);

        transactionService.delete(50L, 99L);

        verify(transactionRepository).delete(tx);
        verify(auditLogService).log(eq(AuditLog.OperationType.TRANSACTION_DELETED),
                eq(AuditLog.EntityType.TRANSACTION), eq(50L), eq(99L), any(), isNull());
    }

    @Test
    void delete_approvedPlannedWithRealizedActualThrows() {
        Season season = openSeason(1L);
        Transaction tx = buildTransaction(50L, season);
        tx.setType(Transaction.TransactionType.PLANNED);
        tx.setStatus(Transaction.ApprovalStatus.APPROVED);
        when(transactionRepository.findById(50L)).thenReturn(Optional.of(tx));
        when(transactionRepository.existsByPlannedTransaction_Id(50L)).thenReturn(true);
        doNothing().when(seasonService).requireOpen(season);

        assertThatThrownBy(() -> transactionService.delete(50L, 99L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("realized actual");

        verify(transactionRepository, never()).delete(any());
        verify(auditLogService, never()).log(eq(AuditLog.OperationType.TRANSACTION_DELETED),
                any(), any(), any(), any(), any());
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
    void reject_proposedPlannedTransaction_setsRejectedStatusAndLogsAudit() {
        Season season = openSeason(1L);
        Transaction tx = buildTransaction(50L, season);
        tx.setType(Transaction.TransactionType.PLANNED);
        tx.setStatus(Transaction.ApprovalStatus.PROPOSED);
        when(transactionRepository.findById(50L)).thenReturn(Optional.of(tx));
        doNothing().when(seasonService).requireOpen(season);
        when(userRepository.findById(99L)).thenReturn(Optional.of(buildUser(99L, User.Role.TREASURER)));
        when(transactionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        TransactionResponse result = transactionService.reject(50L, 99L);

        assertThat(result.getStatus()).isEqualTo(Transaction.ApprovalStatus.REJECTED);
        verify(auditLogService).log(eq(AuditLog.OperationType.TRANSACTION_REJECTED), eq(AuditLog.EntityType.TRANSACTION), eq(50L), eq(99L), any(), any());
    }

    @Test
    void realize_approvedPlannedTransaction_createsApprovedActualLinkedToPlan() {
        Season season = openSeason(1L);
        BudgetItem item = buildItem(10L, BudgetItem.ItemType.EXPENSE, season);
        Transaction planned = buildTransaction(50L, season);
        planned.setType(Transaction.TransactionType.PLANNED);
        planned.setStatus(Transaction.ApprovalStatus.APPROVED);
        planned.setBudgetItem(item);
        when(transactionRepository.findById(50L)).thenReturn(Optional.of(planned));
        doNothing().when(seasonService).requireOpen(season);
        when(userRepository.findById(99L)).thenReturn(Optional.of(buildUser(99L, User.Role.TREASURER)));
        when(transactionRepository.save(any())).thenAnswer(inv -> {
            Transaction t = inv.getArgument(0);
            t.setId(101L);
            return t;
        });

        RealizePlannedTransactionRequest req = new RealizePlannedTransactionRequest();
        req.setDate(LocalDate.of(2024, 10, 5));
        req.setAmount(new BigDecimal("180.00"));
        req.setDescription("Actual paid invoice");

        TransactionResponse result = transactionService.realizePlanned(50L, req, 99L);

        assertThat(result.getType()).isEqualTo(Transaction.TransactionType.ACTUAL);
        assertThat(result.getStatus()).isEqualTo(Transaction.ApprovalStatus.APPROVED);
        assertThat(result.getPlannedTransactionId()).isEqualTo(50L);
    }

    @Test
    void findById_notFound_throwsResourceNotFoundException() {
        when(transactionRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> transactionService.findById(999L, User.Role.ADMIN, null))
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
        t.setDate(LocalDate.of(2024, 10, 1));
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
        r.setDate(LocalDate.of(2024, 10, 1));
        r.setAmount(new BigDecimal("200.00"));
        r.setType(type);
        r.setDirection(direction);
        r.setSeasonId(1L);
        r.setBudgetItemId(budgetItemId);
        return r;
    }

    private User buildUser(Long id, User.Role role) {
        User user = new User();
        user.setId(id);
        user.setUsername("user" + id);
        user.setPasswordHash("hash");
        user.setRole(role);
        return user;
    }

    private UpdateTransactionRequest buildUpdateRequest() {
        UpdateTransactionRequest r = new UpdateTransactionRequest();
        r.setDate(LocalDate.of(2024, 10, 1));
        r.setAmount(new BigDecimal("200.00"));
        r.setDescription("updated");
        return r;
    }
}
