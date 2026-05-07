package cz.cvut.fit.budget_app.service;

import cz.cvut.fit.budget_app.dto.request.CreateBudgetItemRequest;
import cz.cvut.fit.budget_app.dto.request.UpdateBudgetItemRequest;
import cz.cvut.fit.budget_app.dto.response.BudgetItemResponse;
import cz.cvut.fit.budget_app.entity.*;
import cz.cvut.fit.budget_app.exception.ResourceNotFoundException;
import cz.cvut.fit.budget_app.exception.SeasonClosedException;
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
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BudgetItemServiceTest {

    @Mock BudgetItemRepository budgetItemRepository;
    @Mock TransactionRepository transactionRepository;
    @Mock SeasonService seasonService;
    @Mock FundingSourceService fundingSourceService;
    @Mock AuditLogService auditLogService;

    @InjectMocks BudgetItemService budgetItemService;

    @Test
    void create_openSeasonNoFundingSource_success() {
        Season season = openSeason(1L);
        when(seasonService.getOrThrow(1L)).thenReturn(season);
        doNothing().when(seasonService).requireOpen(season);
        when(budgetItemRepository.save(any())).thenAnswer(i -> {
            BudgetItem b = i.getArgument(0);
            b.setId(10L);
            return b;
        });
        when(transactionRepository.findByBudgetItemId(10L)).thenReturn(List.of());

        CreateBudgetItemRequest req = buildRequest(1L, null);
        BudgetItemResponse result = budgetItemService.create(req, 99L);

        assertThat(result.getName()).isEqualTo("Test Item");
        assertThat(result.getBalance()).isEqualByComparingTo(new BigDecimal("500.00"));
        verify(auditLogService).logBudgetItemCreated(10L, 99L);
    }

    @Test
    void create_closedSeason_throwsSeasonClosedException() {
        Season season = openSeason(1L);
        when(seasonService.getOrThrow(1L)).thenReturn(season);
        doThrow(new SeasonClosedException(1L)).when(seasonService).requireOpen(season);

        assertThatThrownBy(() -> budgetItemService.create(buildRequest(1L, null), 99L))
                .isInstanceOf(SeasonClosedException.class);
    }

    @Test
    void create_fundingSourceFromDifferentSeason_throwsIllegalArgument() {
        Season season1 = openSeason(1L);
        Season season2 = openSeason(2L);
        FundingSource fs = buildFundingSource(10L, season2);

        when(seasonService.getOrThrow(1L)).thenReturn(season1);
        doNothing().when(seasonService).requireOpen(season1);
        when(fundingSourceService.getOrThrow(10L)).thenReturn(fs);

        CreateBudgetItemRequest req = buildRequest(1L, 10L);

        assertThatThrownBy(() -> budgetItemService.create(req, 99L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("same season");
    }

    @Test
    void delete_withExistingTransactions_throwsIllegalState() {
        Season season = openSeason(1L);
        BudgetItem item = buildItem(5L, season);
        when(budgetItemRepository.findById(5L)).thenReturn(Optional.of(item));
        doNothing().when(seasonService).requireOpen(season);
        when(transactionRepository.findByBudgetItemId(5L)).thenReturn(List.of(new Transaction()));

        assertThatThrownBy(() -> budgetItemService.delete(5L, 99L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("transaction");
    }

    @Test
    void delete_closedSeason_throwsSeasonClosedException() {
        Season season = openSeason(1L);
        BudgetItem item = buildItem(5L, season);
        when(budgetItemRepository.findById(5L)).thenReturn(Optional.of(item));
        doThrow(new SeasonClosedException(1L)).when(seasonService).requireOpen(season);

        assertThatThrownBy(() -> budgetItemService.delete(5L, 99L))
                .isInstanceOf(SeasonClosedException.class);
    }

    @Test
    void delete_noTransactions_deletesAndLogsAudit() {
        Season season = openSeason(1L);
        BudgetItem item = buildItem(5L, season);
        when(budgetItemRepository.findById(5L)).thenReturn(Optional.of(item));
        doNothing().when(seasonService).requireOpen(season);
        when(transactionRepository.findByBudgetItemId(5L)).thenReturn(List.of());

        budgetItemService.delete(5L, 99L);

        verify(budgetItemRepository).delete(item);
        verify(auditLogService).logBudgetItemDeleted(5L, 99L);
    }

    @Test
    void update_openSeason_updatesItem() {
        Season season = openSeason(1L);
        BudgetItem item = buildItem(5L, season);
        FundingSource fs = buildFundingSource(10L, season);
        when(budgetItemRepository.findById(5L)).thenReturn(Optional.of(item));
        doNothing().when(seasonService).requireOpen(season);
        when(fundingSourceService.getOrThrow(10L)).thenReturn(fs);
        when(budgetItemRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(transactionRepository.findByBudgetItemId(5L)).thenReturn(List.of());

        UpdateBudgetItemRequest req = buildUpdateRequest(10L);
        BudgetItemResponse result = budgetItemService.update(5L, req);

        assertThat(result.getName()).isEqualTo("Updated Item");
        assertThat(result.getPlannedAmount()).isEqualByComparingTo(new BigDecimal("700.00"));
    }

    @Test
    void update_closedSeason_throwsSeasonClosedException() {
        Season season = openSeason(1L);
        BudgetItem item = buildItem(5L, season);
        when(budgetItemRepository.findById(5L)).thenReturn(Optional.of(item));
        doThrow(new SeasonClosedException(1L)).when(seasonService).requireOpen(season);

        assertThatThrownBy(() -> budgetItemService.update(5L, buildUpdateRequest(null)))
                .isInstanceOf(SeasonClosedException.class);
    }

    @Test
    void findById_notFound_throwsResourceNotFoundException() {
        when(budgetItemRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> budgetItemService.findById(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ---- helpers ----

    private Season openSeason(Long id) {
        Season s = new Season();
        s.setId(id);
        s.setName("Season " + id);
        s.setDateFrom(LocalDate.of(2024, 9, 1));
        s.setDateTo(LocalDate.of(2025, 6, 30));
        s.setStatus(Season.SeasonStatus.OPEN);
        return s;
    }

    private BudgetItem buildItem(Long id, Season season) {
        BudgetItem b = new BudgetItem();
        b.setId(id);
        b.setName("Test Item");
        b.setType(BudgetItem.ItemType.EXPENSE);
        b.setPlannedAmount(new BigDecimal("500.00"));
        b.setSeason(season);
        return b;
    }

    private FundingSource buildFundingSource(Long id, Season season) {
        FundingSource fs = new FundingSource();
        fs.setId(id);
        fs.setName("Grant");
        fs.setType(FundingSource.FundingType.PUBLIC_GRANT);
        fs.setSeason(season);
        return fs;
    }

    private CreateBudgetItemRequest buildRequest(Long seasonId, Long fundingSourceId) {
        CreateBudgetItemRequest r = new CreateBudgetItemRequest();
        r.setName("Test Item");
        r.setType(BudgetItem.ItemType.EXPENSE);
        r.setPlannedAmount(new BigDecimal("500.00"));
        r.setSeasonId(seasonId);
        r.setFundingSourceId(fundingSourceId);
        return r;
    }

    private UpdateBudgetItemRequest buildUpdateRequest(Long fundingSourceId) {
        UpdateBudgetItemRequest r = new UpdateBudgetItemRequest();
        r.setName("Updated Item");
        r.setType(BudgetItem.ItemType.EXPENSE);
        r.setPlannedAmount(new BigDecimal("700.00"));
        r.setFundingSourceId(fundingSourceId);
        return r;
    }
}
