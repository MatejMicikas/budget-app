package cz.cvut.fit.budget_app.service;

import cz.cvut.fit.budget_app.dto.request.CreateSeasonRequest;
import cz.cvut.fit.budget_app.dto.request.UpdateSeasonRequest;
import cz.cvut.fit.budget_app.dto.response.SeasonResponse;
import cz.cvut.fit.budget_app.entity.Season;
import cz.cvut.fit.budget_app.exception.ResourceNotFoundException;
import cz.cvut.fit.budget_app.exception.SeasonClosedException;
import cz.cvut.fit.budget_app.repository.SeasonRepository;
import cz.cvut.fit.budget_app.repository.TransactionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SeasonServiceTest {

    @Mock SeasonRepository seasonRepository;
    @Mock TransactionRepository transactionRepository;
    @Mock AuditLogService auditLogService;

    @InjectMocks SeasonService seasonService;

    @Test
    void create_validDates_returnsSeason() {
        CreateSeasonRequest req = buildRequest("2024/2025",
                LocalDate.of(2024, 9, 1), LocalDate.of(2025, 6, 30));
        when(seasonRepository.save(any())).thenAnswer(i -> {
            Season s = i.getArgument(0);
            s.setId(1L);
            return s;
        });

        SeasonResponse result = seasonService.create(req, 99L);

        assertThat(result.getName()).isEqualTo("2024/2025");
        assertThat(result.getStatus()).isEqualTo(Season.SeasonStatus.OPEN);
        assertThat(result.isMemberSummaryVisible()).isTrue();
        verify(auditLogService).logSeasonCreated(eq(1L), eq(99L), any());
    }

    @Test
    void create_dateToNotAfterDateFrom_throwsIllegalArgument() {
        CreateSeasonRequest req = buildRequest("bad",
                LocalDate.of(2024, 9, 1), LocalDate.of(2024, 9, 1));

        assertThatThrownBy(() -> seasonService.create(req, 99L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("dateTo must be after dateFrom");
    }

    @Test
    void findAll_returnsMappedList() {
        Season s1 = buildSeason(1L, Season.SeasonStatus.OPEN);
        Season s2 = buildSeason(2L, Season.SeasonStatus.CLOSED);
        when(seasonRepository.findAll()).thenReturn(List.of(s1, s2));

        List<SeasonResponse> result = seasonService.findAll();

        assertThat(result).hasSize(2);
    }

    @Test
    void close_openSeason_setsClosedAndLogsAudit() {
        Season season = buildSeason(1L, Season.SeasonStatus.OPEN);
        when(seasonRepository.findById(1L)).thenReturn(Optional.of(season));
        when(seasonRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        SeasonResponse result = seasonService.close(1L, 99L);

        assertThat(result.getStatus()).isEqualTo(Season.SeasonStatus.CLOSED);
        verify(auditLogService).logSeasonClosed(1L, 99L);
    }

    @Test
    void close_alreadyClosed_throwsSeasonClosedException() {
        Season season = buildSeason(1L, Season.SeasonStatus.CLOSED);
        when(seasonRepository.findById(1L)).thenReturn(Optional.of(season));

        assertThatThrownBy(() -> seasonService.close(1L, 99L))
                .isInstanceOf(SeasonClosedException.class);

        verify(auditLogService, never()).logSeasonClosed(anyLong(), anyLong());
    }

    @Test
    void findById_notFound_throwsResourceNotFoundException() {
        when(seasonRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> seasonService.findById(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void requireOpen_closedSeason_throws() {
        Season closed = buildSeason(5L, Season.SeasonStatus.CLOSED);

        assertThatThrownBy(() -> seasonService.requireOpen(closed))
                .isInstanceOf(SeasonClosedException.class);
    }

    @Test
    void update_openSeason_updatesFields() {
        Season season = buildSeason(1L, Season.SeasonStatus.OPEN);
        when(seasonRepository.findById(1L)).thenReturn(Optional.of(season));
        when(seasonRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        UpdateSeasonRequest req = new UpdateSeasonRequest();
        req.setName("Updated Season");
        req.setDateFrom(LocalDate.of(2024, 8, 1));
        req.setDateTo(LocalDate.of(2025, 7, 1));
        req.setMemberSummaryVisible(false);

        SeasonResponse result = seasonService.update(1L, req, 99L);

        assertThat(result.getName()).isEqualTo("Updated Season");
        assertThat(result.getDateFrom()).isEqualTo(LocalDate.of(2024, 8, 1));
        assertThat(result.isMemberSummaryVisible()).isFalse();
        verify(auditLogService).logSeasonUpdated(eq(1L), eq(99L), any(), any());
    }

    @Test
    void update_newRangeExcludesTransactions_throwsIllegalArgument() {
        Season season = buildSeason(1L, Season.SeasonStatus.OPEN);
        when(seasonRepository.findById(1L)).thenReturn(Optional.of(season));
        when(transactionRepository.existsBySeasonIdAndDateLessThan(eq(1L), eq(LocalDate.of(2026, 1, 1))))
                .thenReturn(true);

        UpdateSeasonRequest req = new UpdateSeasonRequest();
        req.setName(season.getName());
        req.setDateFrom(LocalDate.of(2026, 1, 1));
        req.setDateTo(LocalDate.of(2026, 5, 31));

        assertThatThrownBy(() -> seasonService.update(1L, req, 99L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("transactions");

        verify(seasonRepository, never()).save(any());
    }

    @Test
    void update_sameDates_doesNotQueryTransactionBounds() {
        Season season = buildSeason(1L, Season.SeasonStatus.OPEN);
        when(seasonRepository.findById(1L)).thenReturn(Optional.of(season));
        when(seasonRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        UpdateSeasonRequest req = new UpdateSeasonRequest();
        req.setName("Renamed only");
        req.setDateFrom(season.getDateFrom());
        req.setDateTo(season.getDateTo());

        seasonService.update(1L, req, 99L);

        verify(transactionRepository, never()).existsBySeasonIdAndDateLessThan(anyLong(), any());
        verify(transactionRepository, never()).existsBySeasonIdAndDateGreaterThan(anyLong(), any());
    }

    // ---- helpers ----

    private Season buildSeason(Long id, Season.SeasonStatus status) {
        Season s = new Season();
        s.setId(id);
        s.setName("Test Season");
        s.setDateFrom(LocalDate.of(2024, 9, 1));
        s.setDateTo(LocalDate.of(2025, 6, 30));
        s.setStatus(status);
        return s;
    }

    private CreateSeasonRequest buildRequest(String name, LocalDate from, LocalDate to) {
        CreateSeasonRequest r = new CreateSeasonRequest();
        r.setName(name);
        r.setDateFrom(from);
        r.setDateTo(to);
        return r;
    }
}
