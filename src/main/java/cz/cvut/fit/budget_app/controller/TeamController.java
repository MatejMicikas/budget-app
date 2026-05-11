package cz.cvut.fit.budget_app.controller;

import cz.cvut.fit.budget_app.dto.request.CreateTeamRequest;
import cz.cvut.fit.budget_app.dto.request.UpdateTeamRequest;
import cz.cvut.fit.budget_app.dto.response.TeamResponse;
import cz.cvut.fit.budget_app.security.UserPrincipal;
import cz.cvut.fit.budget_app.service.TeamService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/teams")
@RequiredArgsConstructor
public class TeamController {

    private final TeamService teamService;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<TeamResponse>> findAll() {
        return ResponseEntity.ok(teamService.findAll());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<TeamResponse> findById(@PathVariable Long id) {
        return ResponseEntity.ok(teamService.findById(id));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<TeamResponse> create(@Valid @RequestBody CreateTeamRequest request,
                                               @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.status(HttpStatus.CREATED).body(teamService.create(request, principal.getId()));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<TeamResponse> update(@PathVariable Long id,
                                               @Valid @RequestBody UpdateTeamRequest request,
                                               @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(teamService.update(id, request, principal.getId()));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable Long id,
                                       @AuthenticationPrincipal UserPrincipal principal) {
        teamService.delete(id, principal.getId());
        return ResponseEntity.noContent().build();
    }
}
