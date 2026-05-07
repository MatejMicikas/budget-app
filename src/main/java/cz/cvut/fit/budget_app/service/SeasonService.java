package cz.cvut.fit.budget_app.service;

import cz.cvut.fit.budget_app.dto.request.CreateSeasonRequest;
import cz.cvut.fit.budget_app.dto.response.SeasonResponse;
import cz.cvut.fit.budget_app.entity.Season;
import cz.cvut.fit.budget_app.exception.ResourceNotFoundException;
import cz.cvut.fit.budget_app.exception.SeasonClosedException;
import cz.cvut.fit.budget_app.repository.SeasonRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SeasonService {

    private final SeasonRepository seasonRepository;
    private final AuditLogService auditLogService;

    @Transactional
    public SeasonResponse create(CreateSeasonRequest request) {
        if (request.getDateTo().isBefore(request.getDateFrom()) ||
                request.getDateTo().isEqual(request.getDateFrom())) {
            throw new IllegalArgumentException("dateTo must be after dateFrom");
        }

        Season season = new Season();
        season.setName(request.getName());
        season.setDateFrom(request.getDateFrom());
        season.setDateTo(request.getDateTo());
        season.setStatus(Season.SeasonStatus.OPEN);

        return SeasonResponse.from(seasonRepository.save(season));
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

    public Season getOrThrow(Long id) {
        return seasonRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Season", id));
    }

    public void requireOpen(Season season) {
        if (season.getStatus() == Season.SeasonStatus.CLOSED) {
            throw new SeasonClosedException(season.getId());
        }
    }
}
