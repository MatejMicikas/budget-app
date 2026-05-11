package cz.cvut.fit.budget_app.service;

import cz.cvut.fit.budget_app.dto.request.CreateFundingSourceRequest;
import cz.cvut.fit.budget_app.dto.request.UpdateFundingSourceRequest;
import cz.cvut.fit.budget_app.entity.BudgetItem;
import cz.cvut.fit.budget_app.entity.FundingSource;
import cz.cvut.fit.budget_app.entity.Season;
import cz.cvut.fit.budget_app.entity.Transaction;
import cz.cvut.fit.budget_app.exception.SeasonClosedException;
import cz.cvut.fit.budget_app.repository.BudgetItemRepository;
import cz.cvut.fit.budget_app.repository.FundingSourceRepository;
import cz.cvut.fit.budget_app.repository.TransactionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FundingSourceServiceTest {

    @Mock FundingSourceRepository fundingSourceRepository;
    @Mock BudgetItemRepository budgetItemRepository;
    @Mock TransactionRepository transactionRepository;
    @Mock SeasonService seasonService;
    @Mock AuditLogService auditLogService;

    @InjectMocks FundingSourceService fundingSourceService;

    @Test
    void update_allocatedAmountBelowActualSpending_throwsIllegalArgumentException() {
        Season season = openSeason(1L);
        FundingSource fs = fundingSource(7L, season);
        BudgetItem item = budgetItem(10L, season, fs);
        Transaction tx = actualExpenseTx(item, "1500.00");

        when(fundingSourceRepository.findById(7L)).thenReturn(Optional.of(fs));
        doNothing().when(seasonService).requireOpen(season);
        when(budgetItemRepository.findByFundingSourceId(7L)).thenReturn(List.of(item));
        when(transactionRepository.findByBudgetItemId(10L)).thenReturn(List.of(tx));

        UpdateFundingSourceRequest req = new UpdateFundingSourceRequest();
        req.setName("Updated");
        req.setType(FundingSource.FundingType.PUBLIC_GRANT);
        req.setAllocatedAmount(new BigDecimal("1000.00"));

        assertThatThrownBy(() -> fundingSourceService.update(7L, req, 99L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("cannot be lower than actual spending");
    }

    @Test
    void create_closedSeason_throwsSeasonClosedException() {
        Season season = openSeason(1L);
        when(seasonService.getOrThrow(1L)).thenReturn(season);
        doThrow(new SeasonClosedException(1L)).when(seasonService).requireOpen(season);

        CreateFundingSourceRequest req = new CreateFundingSourceRequest();
        req.setName("Grant");
        req.setType(FundingSource.FundingType.PUBLIC_GRANT);
        req.setSeasonId(1L);

        assertThatThrownBy(() -> fundingSourceService.create(req, 99L))
                .isInstanceOf(SeasonClosedException.class);
    }

    @Test
    void update_closedSeason_throwsSeasonClosedException() {
        Season season = openSeason(1L);
        FundingSource fs = fundingSource(7L, season);
        when(fundingSourceRepository.findById(7L)).thenReturn(Optional.of(fs));
        doThrow(new SeasonClosedException(1L)).when(seasonService).requireOpen(season);

        UpdateFundingSourceRequest req = new UpdateFundingSourceRequest();
        req.setName("Updated");
        req.setType(FundingSource.FundingType.PUBLIC_GRANT);
        req.setAllocatedAmount(new BigDecimal("3000.00"));

        assertThatThrownBy(() -> fundingSourceService.update(7L, req, 99L))
                .isInstanceOf(SeasonClosedException.class);
    }

    @Test
    void delete_closedSeason_throwsSeasonClosedException() {
        Season season = openSeason(1L);
        FundingSource fs = fundingSource(7L, season);
        when(fundingSourceRepository.findById(7L)).thenReturn(Optional.of(fs));
        doThrow(new SeasonClosedException(1L)).when(seasonService).requireOpen(season);

        assertThatThrownBy(() -> fundingSourceService.delete(7L, 99L))
                .isInstanceOf(SeasonClosedException.class);
    }

    @Test
    void update_ignoresNonApprovedTransactionsInActualSpending() {
        Season season = openSeason(1L);
        FundingSource fs = fundingSource(7L, season);
        BudgetItem item = budgetItem(10L, season, fs);

        Transaction approvedActual = actualExpenseTx(item, "900.00");
        approvedActual.setStatus(Transaction.ApprovalStatus.APPROVED);
        Transaction proposedActual = actualExpenseTx(item, "900.00");
        proposedActual.setStatus(Transaction.ApprovalStatus.PROPOSED);

        when(fundingSourceRepository.findById(7L)).thenReturn(Optional.of(fs));
        doNothing().when(seasonService).requireOpen(season);
        when(budgetItemRepository.findByFundingSourceId(7L)).thenReturn(List.of(item));
        when(transactionRepository.findByBudgetItemId(10L)).thenReturn(List.of(approvedActual, proposedActual));
        when(fundingSourceRepository.save(fs)).thenReturn(fs);

        UpdateFundingSourceRequest req = new UpdateFundingSourceRequest();
        req.setName("Updated");
        req.setType(FundingSource.FundingType.PUBLIC_GRANT);
        req.setAllocatedAmount(new BigDecimal("1000.00"));

        assertThatCode(() -> fundingSourceService.update(7L, req, 99L))
                .doesNotThrowAnyException();
    }

    private Season openSeason(Long id) {
        Season s = new Season();
        s.setId(id);
        s.setName("Season");
        s.setDateFrom(LocalDate.of(2024, 9, 1));
        s.setDateTo(LocalDate.of(2025, 6, 30));
        s.setStatus(Season.SeasonStatus.OPEN);
        return s;
    }

    private FundingSource fundingSource(Long id, Season season) {
        FundingSource fs = new FundingSource();
        fs.setId(id);
        fs.setName("Grant");
        fs.setType(FundingSource.FundingType.PUBLIC_GRANT);
        fs.setAllocatedAmount(new BigDecimal("2000.00"));
        fs.setSeason(season);
        return fs;
    }

    private BudgetItem budgetItem(Long id, Season season, FundingSource fs) {
        BudgetItem item = new BudgetItem();
        item.setId(id);
        item.setName("Item");
        item.setType(BudgetItem.ItemType.EXPENSE);
        item.setPlannedAmount(new BigDecimal("2000.00"));
        item.setSeason(season);
        item.setFundingSource(fs);
        return item;
    }

    private Transaction actualExpenseTx(BudgetItem item, String amount) {
        Transaction tx = new Transaction();
        tx.setId(1L);
        tx.setDate(LocalDate.of(2024, 10, 1));
        tx.setType(Transaction.TransactionType.ACTUAL);
        tx.setDirection(Transaction.Direction.EXPENSE);
        tx.setAmount(new BigDecimal(amount));
        tx.setBudgetItem(item);
        tx.setSeason(item.getSeason());
        tx.setStatus(Transaction.ApprovalStatus.APPROVED);
        return tx;
    }
}
