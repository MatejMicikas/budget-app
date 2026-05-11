package cz.cvut.fit.budget_app.controller;

import cz.cvut.fit.budget_app.dto.request.CreateSeasonRequest;
import cz.cvut.fit.budget_app.dto.request.UpdateSeasonRequest;
import cz.cvut.fit.budget_app.dto.response.SeasonResponse;
import cz.cvut.fit.budget_app.security.UserPrincipal;
import cz.cvut.fit.budget_app.service.SeasonService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/seasons")
@RequiredArgsConstructor
public class SeasonController {

    private final SeasonService seasonService;

    @GetMapping
    public ResponseEntity<List<SeasonResponse>> findAll() {
        return ResponseEntity.ok(seasonService.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<SeasonResponse> findById(@PathVariable Long id) {
        return ResponseEntity.ok(seasonService.findById(id));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<SeasonResponse> create(@Valid @RequestBody CreateSeasonRequest request,
                                                 @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.status(HttpStatus.CREATED).body(seasonService.create(request, principal.getId()));
    }

    @PostMapping("/{id}/close")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<SeasonResponse> close(@PathVariable Long id,
                                                 @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(seasonService.close(id, principal.getId()));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<SeasonResponse> update(@PathVariable Long id,
                                                 @Valid @RequestBody UpdateSeasonRequest request,
                                                 @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(seasonService.update(id, request, principal.getId()));
    }
}
