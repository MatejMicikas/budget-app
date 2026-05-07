package cz.cvut.fit.budget_app.controller;

import cz.cvut.fit.budget_app.dto.request.CreateTransactionRequest;
import cz.cvut.fit.budget_app.dto.response.TransactionResponse;
import cz.cvut.fit.budget_app.entity.Transaction;
import cz.cvut.fit.budget_app.entity.User;
import cz.cvut.fit.budget_app.security.UserPrincipal;
import cz.cvut.fit.budget_app.service.TransactionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/transactions")
@RequiredArgsConstructor
public class TransactionController {

    private final TransactionService transactionService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'TREASURER', 'TEAM_LEADER')")
    public ResponseEntity<List<TransactionResponse>> find(
            @RequestParam(required = false) Long seasonId,
            @RequestParam(required = false) Long fundingSourceId,
            @RequestParam(required = false) Transaction.TransactionType type,
            @RequestParam(required = false) Long budgetItemId) {
        if (fundingSourceId != null) {
            return ResponseEntity.ok(transactionService.findByFundingSourceId(fundingSourceId));
        }
        if (seasonId == null) {
            throw new IllegalArgumentException("Either seasonId or fundingSourceId must be provided");
        }
        return ResponseEntity.ok(transactionService.findBySeasonWithFilters(seasonId, type, budgetItemId));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'TREASURER', 'TEAM_LEADER')")
    public ResponseEntity<TransactionResponse> findById(@PathVariable Long id) {
        return ResponseEntity.ok(transactionService.findById(id));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'TREASURER', 'TEAM_LEADER')")
    public ResponseEntity<TransactionResponse> create(@Valid @RequestBody CreateTransactionRequest request,
                                                       @AuthenticationPrincipal UserPrincipal principal) {
        // TEAM_LEADER smí vytvářet pouze PLANNED (navrhované) transakce
        if (principal.getRole() == User.Role.TEAM_LEADER
                && request.getType() != Transaction.TransactionType.PLANNED) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(transactionService.create(request, principal.getId()));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'TREASURER')")
    public ResponseEntity<Void> delete(@PathVariable Long id,
                                        @AuthenticationPrincipal UserPrincipal principal) {
        transactionService.delete(id, principal.getId());
        return ResponseEntity.noContent().build();
    }
}
