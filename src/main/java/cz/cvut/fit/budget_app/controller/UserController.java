package cz.cvut.fit.budget_app.controller;

import cz.cvut.fit.budget_app.dto.request.ChangeRoleRequest;
import cz.cvut.fit.budget_app.dto.request.CreateUserRequest;
import cz.cvut.fit.budget_app.dto.response.UserResponse;
import cz.cvut.fit.budget_app.security.UserPrincipal;
import cz.cvut.fit.budget_app.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<UserResponse>> findAll() {
        return ResponseEntity.ok(userService.findAll());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserResponse> findById(@PathVariable Long id) {
        return ResponseEntity.ok(userService.findById(id));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserResponse> create(@Valid @RequestBody CreateUserRequest request,
                                             @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(userService.create(request, principal.getId()));
    }

    @PutMapping("/{id}/role")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserResponse> changeRole(@PathVariable Long id,
                                                    @Valid @RequestBody ChangeRoleRequest request,
                                                    @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(userService.changeRole(id, request, principal.getId()));
    }

    @PutMapping("/{id}/team/{teamId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserResponse> assignTeam(@PathVariable Long id,
                                                   @PathVariable Long teamId,
                                                   @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(userService.assignTeam(id, teamId, principal.getId()));
    }

    @DeleteMapping("/{id}/team")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserResponse> unassignTeam(@PathVariable Long id,
                                                     @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(userService.unassignTeam(id, principal.getId()));
    }
}
