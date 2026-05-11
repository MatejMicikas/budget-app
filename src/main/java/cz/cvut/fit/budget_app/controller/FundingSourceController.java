package cz.cvut.fit.budget_app.controller;

import cz.cvut.fit.budget_app.dto.request.CreateFundingSourceRequest;
import cz.cvut.fit.budget_app.dto.request.UpdateFundingSourceRequest;
import cz.cvut.fit.budget_app.dto.response.FundingSourceResponse;
import cz.cvut.fit.budget_app.security.UserPrincipal;
import cz.cvut.fit.budget_app.service.FundingSourceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/funding-sources")
@RequiredArgsConstructor
public class FundingSourceController {

    private final FundingSourceService fundingSourceService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'TREASURER', 'TEAM_LEADER')")
    public ResponseEntity<List<FundingSourceResponse>> findBySeason(@RequestParam Long seasonId,
                                                                    @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(fundingSourceService.findBySeasonId(
                seasonId, principal.getRole(), principal.getTeamId()));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'TREASURER', 'TEAM_LEADER')")
    public ResponseEntity<FundingSourceResponse> findById(@PathVariable Long id,
                                                          @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(fundingSourceService.findById(id, principal.getRole(), principal.getTeamId()));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'TREASURER')")
    public ResponseEntity<FundingSourceResponse> create(@Valid @RequestBody CreateFundingSourceRequest request,
                                                         @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.status(HttpStatus.CREATED).body(fundingSourceService.create(request, principal.getId()));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'TREASURER')")
    public ResponseEntity<FundingSourceResponse> update(@PathVariable Long id,
                                                        @Valid @RequestBody UpdateFundingSourceRequest request,
                                                        @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(fundingSourceService.update(id, request, principal.getId()));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'TREASURER')")
    public ResponseEntity<Void> delete(@PathVariable Long id,
                                       @AuthenticationPrincipal UserPrincipal principal) {
        fundingSourceService.delete(id, principal.getId());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/spending-report")
    @PreAuthorize("hasAnyRole('ADMIN', 'TREASURER', 'TEAM_LEADER')")
    public ResponseEntity<FundingSourceResponse> spendingReport(@PathVariable Long id,
                                                                @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(fundingSourceService.findById(id, principal.getRole(), principal.getTeamId()));
    }
}
