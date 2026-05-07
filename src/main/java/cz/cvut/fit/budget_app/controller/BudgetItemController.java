package cz.cvut.fit.budget_app.controller;

import cz.cvut.fit.budget_app.dto.request.CreateBudgetItemRequest;
import cz.cvut.fit.budget_app.dto.request.UpdateBudgetItemRequest;
import cz.cvut.fit.budget_app.dto.response.BudgetItemResponse;
import cz.cvut.fit.budget_app.security.UserPrincipal;
import cz.cvut.fit.budget_app.service.BudgetItemService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/budget-items")
@RequiredArgsConstructor
public class BudgetItemController {

    private final BudgetItemService budgetItemService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'TREASURER', 'TEAM_LEADER')")
    public ResponseEntity<List<BudgetItemResponse>> findBySeason(@RequestParam Long seasonId) {
        return ResponseEntity.ok(budgetItemService.findBySeasonId(seasonId));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'TREASURER', 'TEAM_LEADER')")
    public ResponseEntity<BudgetItemResponse> findById(@PathVariable Long id) {
        return ResponseEntity.ok(budgetItemService.findById(id));
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
                                                     @Valid @RequestBody UpdateBudgetItemRequest request) {
        return ResponseEntity.ok(budgetItemService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'TREASURER')")
    public ResponseEntity<Void> delete(@PathVariable Long id,
                                        @AuthenticationPrincipal UserPrincipal principal) {
        budgetItemService.delete(id, principal.getId());
        return ResponseEntity.noContent().build();
    }
}
