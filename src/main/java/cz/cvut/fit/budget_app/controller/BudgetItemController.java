package cz.cvut.fit.budget_app.controller;

import cz.cvut.fit.budget_app.dto.request.CreateBudgetItemRequest;
import cz.cvut.fit.budget_app.dto.request.UpdateBudgetItemRequest;
import cz.cvut.fit.budget_app.dto.response.BudgetItemResponse;
import cz.cvut.fit.budget_app.dto.response.BudgetSummaryResponse;
import cz.cvut.fit.budget_app.entity.BudgetItem;
import cz.cvut.fit.budget_app.entity.Transaction;
import cz.cvut.fit.budget_app.entity.User;
import cz.cvut.fit.budget_app.security.UserPrincipal;
import cz.cvut.fit.budget_app.service.BudgetItemService;
import cz.cvut.fit.budget_app.service.BudgetSummaryService;
import cz.cvut.fit.budget_app.service.SeasonService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/budget-items")
@RequiredArgsConstructor
public class BudgetItemController {

    private final BudgetItemService budgetItemService;
    private final BudgetSummaryService budgetSummaryService;
    private final SeasonService seasonService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'TREASURER', 'TEAM_LEADER')")
    public ResponseEntity<List<BudgetItemResponse>> findBySeason(@RequestParam Long seasonId,
                                                                 @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(budgetItemService.findBySeasonId(
                seasonId, principal.getRole(), principal.getTeamId()));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'TREASURER', 'TEAM_LEADER')")
    public ResponseEntity<BudgetItemResponse> findById(@PathVariable Long id,
                                                       @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(budgetItemService.findById(id, principal.getRole(), principal.getTeamId()));
    }

    @GetMapping("/summary")
    @PreAuthorize("hasAnyRole('ADMIN', 'TREASURER', 'TEAM_LEADER', 'MEMBER')")
    public ResponseEntity<BudgetSummaryResponse> getSummary(@RequestParam Long seasonId,
                                                            @RequestParam(required = false) BudgetItem.ItemType itemType,
                                                            @RequestParam(required = false) Transaction.TransactionType transactionType,
                                                            @AuthenticationPrincipal UserPrincipal principal) {
        if (principal.getRole() == User.Role.MEMBER
                && !seasonService.getOrThrow(seasonId).isMemberSummaryVisible()) {
            throw new AccessDeniedException("Summary is not available for members in this season");
        }
        if (principal.getRole() == User.Role.TEAM_LEADER) {
            if (principal.getTeamId() == null) {
                throw new IllegalStateException("TEAM_LEADER must belong to a team");
            }
            return ResponseEntity.ok(budgetSummaryService.getSeasonSummaryForTeam(
                    seasonId, principal.getTeamId(), itemType, transactionType));
        }
        return ResponseEntity.ok(budgetSummaryService.getSeasonSummary(seasonId, itemType, transactionType));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'TREASURER')")
    public ResponseEntity<BudgetItemResponse> create(@Valid @RequestBody CreateBudgetItemRequest request,
                                                      @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(budgetItemService.create(request, principal.getId()));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'TREASURER')")
    public ResponseEntity<BudgetItemResponse> update(@PathVariable Long id,
                                                     @Valid @RequestBody UpdateBudgetItemRequest request,
                                                     @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(budgetItemService.update(id, request, principal.getId()));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'TREASURER')")
    public ResponseEntity<Void> delete(@PathVariable Long id,
                                        @AuthenticationPrincipal UserPrincipal principal) {
        budgetItemService.delete(id, principal.getId());
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}/team/{teamId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<BudgetItemResponse> assignTeam(@PathVariable Long id,
                                                         @PathVariable Long teamId,
                                                         @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(budgetItemService.assignTeam(id, teamId, principal.getId()));
    }

    @DeleteMapping("/{id}/team")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<BudgetItemResponse> unassignTeam(@PathVariable Long id,
                                                           @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(budgetItemService.unassignTeam(id, principal.getId()));
    }
}
