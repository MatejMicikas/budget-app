package cz.cvut.fit.budget_app.service;

import cz.cvut.fit.budget_app.dto.request.CreateSeasonRequest;
import cz.cvut.fit.budget_app.dto.request.UpdateSeasonRequest;
import cz.cvut.fit.budget_app.dto.response.SeasonResponse;
import cz.cvut.fit.budget_app.entity.Season;
import cz.cvut.fit.budget_app.exception.ResourceNotFoundException;
import cz.cvut.fit.budget_app.exception.SeasonClosedException;
import cz.cvut.fit.budget_app.repository.SeasonRepository;
import cz.cvut.fit.budget_app.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class SeasonService {

    private final SeasonRepository seasonRepository;
    private final TransactionRepository transactionRepository;
    private final AuditLogService auditLogService;

    @Transactional
    public SeasonResponse create(CreateSeasonRequest request, Long performedByUserId) {
        if (request.getDateTo().isBefore(request.getDateFrom()) ||
                request.getDateTo().isEqual(request.getDateFrom())) {
            throw new IllegalArgumentException("dateTo must be after dateFrom");
        }

        Season season = new Season();
        season.setName(request.getName());
        season.setDateFrom(request.getDateFrom());
        season.setDateTo(request.getDateTo());
        season.setStatus(Season.SeasonStatus.OPEN);
        season.setMemberSummaryVisible(request.getMemberSummaryVisible() == null || request.getMemberSummaryVisible());

        Season saved = seasonRepository.save(season);
        String newValueJson = AuditSnapshotSerializer.toJson(seasonCreatedSnapshot(saved));
        auditLogService.logSeasonCreated(saved.getId(), performedByUserId, newValueJson);
        return SeasonResponse.from(saved);
    }

    @Transactional(readOnly = true)
    public SeasonResponse findById(Long id) {
        return SeasonResponse.from(getOrThrow(id));
    }

    @Transactional(readOnly = true)
    public List<SeasonResponse> findAll() {
        return seasonRepository.findAll().stream()
                .map(SeasonResponse::from)
                .toList();
    }

    @Transactional
    public SeasonResponse close(Long seasonId, Long performedByUserId) {
        Season season = getOrThrow(seasonId);

        if (season.getStatus() == Season.SeasonStatus.CLOSED) {
            throw new SeasonClosedException(seasonId);
        }

        season.setStatus(Season.SeasonStatus.CLOSED);
        Season saved = seasonRepository.save(season);

        auditLogService.logSeasonClosed(seasonId, performedByUserId);

        return SeasonResponse.from(saved);
    }

    @Transactional
    public SeasonResponse update(Long seasonId, UpdateSeasonRequest request, Long performedByUserId) {
        if (request.getDateTo().isBefore(request.getDateFrom()) ||
                request.getDateTo().isEqual(request.getDateFrom())) {
            throw new IllegalArgumentException("dateTo must be after dateFrom");
        }

        Season season = getOrThrow(seasonId);
        requireOpen(season);

        boolean datesChanged = !Objects.equals(season.getDateFrom(), request.getDateFrom())
                || !Objects.equals(season.getDateTo(), request.getDateTo());
        if (datesChanged
                && (transactionRepository.existsBySeasonIdAndDateLessThan(seasonId, request.getDateFrom())
                || transactionRepository.existsBySeasonIdAndDateGreaterThan(seasonId, request.getDateTo()))) {
            throw new IllegalArgumentException(
                    "Cannot change season dates: existing transactions must stay within the new date range");
        }

        String oldValueJson = AuditSnapshotSerializer.toJson(seasonUpdateSnapshot(season));

        season.setName(request.getName());
        season.setDateFrom(request.getDateFrom());
        season.setDateTo(request.getDateTo());
        if (request.getMemberSummaryVisible() != null) {
            season.setMemberSummaryVisible(request.getMemberSummaryVisible());
        }
        Season saved = seasonRepository.save(season);
        String newValueJson = AuditSnapshotSerializer.toJson(seasonUpdateSnapshot(saved));
        auditLogService.logSeasonUpdated(saved.getId(), performedByUserId, oldValueJson, newValueJson);
        return SeasonResponse.from(saved);
    }

    public Season getOrThrow(Long id) {
        return seasonRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Season", id));
    }

    public void requireOpen(Season season) {
        if (season.getStatus() == Season.SeasonStatus.CLOSED) {
            throw new SeasonClosedException(season.getId());
        }
    }

    private static Map<String, Object> seasonCreatedSnapshot(Season s) {
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("name", s.getName());
        snapshot.put("dateFrom", s.getDateFrom());
        snapshot.put("dateTo", s.getDateTo());
        snapshot.put("status", s.getStatus());
        return snapshot;
    }

    private static Map<String, Object> seasonUpdateSnapshot(Season s) {
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("name", s.getName());
        snapshot.put("dateFrom", s.getDateFrom());
        snapshot.put("dateTo", s.getDateTo());
        snapshot.put("memberSummaryVisible", s.isMemberSummaryVisible());
        return snapshot;
    }
}
