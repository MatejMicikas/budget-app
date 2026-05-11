package cz.cvut.fit.budget_app.controller;

import cz.cvut.fit.budget_app.dto.response.SeasonFundingSpendingReportResponse;
import cz.cvut.fit.budget_app.security.UserPrincipal;
import cz.cvut.fit.budget_app.service.FundingSourceService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/seasons")
@RequiredArgsConstructor
public class SeasonFundingReportController {

    private final FundingSourceService fundingSourceService;

    @GetMapping("/{seasonId}/funding-sources/spending-report")
    @PreAuthorize("hasAnyRole('ADMIN', 'TREASURER', 'TEAM_LEADER')")
    public ResponseEntity<SeasonFundingSpendingReportResponse> getSeasonFundingSpendingReport(
            @PathVariable Long seasonId,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(fundingSourceService.getSeasonSpendingReport(
                seasonId,
                principal.getRole(),
                principal.getTeamId()));
    }
}
