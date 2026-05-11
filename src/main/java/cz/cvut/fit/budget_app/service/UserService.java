package cz.cvut.fit.budget_app.service;

import cz.cvut.fit.budget_app.dto.request.ChangeRoleRequest;
import cz.cvut.fit.budget_app.dto.request.CreateUserRequest;
import cz.cvut.fit.budget_app.dto.response.UserResponse;
import cz.cvut.fit.budget_app.entity.Team;
import cz.cvut.fit.budget_app.entity.User;
import cz.cvut.fit.budget_app.exception.ResourceNotFoundException;
import cz.cvut.fit.budget_app.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final AuditLogService auditLogService;
    private final PasswordEncoder passwordEncoder;
    private final TeamService teamService;

    @Transactional
    public UserResponse create(CreateUserRequest request, Long performedByUserId) {
        if (userRepository.findByUsername(request.getUsername()).isPresent()) {
            throw new IllegalArgumentException("Username '" + request.getUsername() + "' is already taken");
        }

        User user = new User();
        user.setUsername(request.getUsername());
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setRole(request.getRole());

        User saved = AuditExecutor.execute(
                auditLogService,
                cz.cvut.fit.budget_app.entity.AuditLog.OperationType.USER_CREATED,
                cz.cvut.fit.budget_app.entity.AuditLog.EntityType.USER,
                performedByUserId,
                null,
                () -> userRepository.save(user),
                User::getId,
                this::userSnapshotJson
        );

        return UserResponse.from(saved);
    }

    private String userSnapshotJson(User user) {
        return AuditSnapshotSerializer.toJson(userSnapshot(user));
    }

    private Map<String, Object> userSnapshot(User user) {
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("username", user.getUsername());
        snapshot.put("role", user.getRole());
        snapshot.put("teamId", user.getTeam() != null ? user.getTeam().getId() : null);
        return snapshot;
    }

    @Transactional(readOnly = true)
    public UserResponse findById(Long id) {
        return UserResponse.from(getOrThrow(id));
    }

    @Transactional(readOnly = true)
    public List<UserResponse> findAll() {
        return userRepository.findAll().stream()
                .map(UserResponse::from)
                .toList();
    }

    @Transactional
    public UserResponse changeRole(Long userId, ChangeRoleRequest request, Long performedByUserId) {
        User user = getOrThrow(userId);
        User.Role oldRole = user.getRole();

        if (performedByUserId.equals(userId)
                && oldRole == User.Role.ADMIN
                && request.getRole() != User.Role.ADMIN) {
            throw new IllegalStateException("ADMIN cannot change their own role");
        }
        if (oldRole == User.Role.ADMIN
                && request.getRole() != User.Role.ADMIN
                && userRepository.countByRole(User.Role.ADMIN) <= 1) {
            throw new IllegalStateException("At least one ADMIN must remain in the system");
        }

        String oldValueJson = AuditSnapshotSerializer.toJson(Map.of("role", oldRole));
        user.setRole(request.getRole());
        User saved = AuditExecutor.execute(
                auditLogService,
                cz.cvut.fit.budget_app.entity.AuditLog.OperationType.USER_ROLE_CHANGED,
                cz.cvut.fit.budget_app.entity.AuditLog.EntityType.USER,
                performedByUserId,
                oldValueJson,
                () -> userRepository.save(user),
                User::getId,
                u -> AuditSnapshotSerializer.toJson(Map.of("role", u.getRole()))
        );

        return UserResponse.from(saved);
    }

    @Transactional
    public UserResponse assignTeam(Long userId, Long teamId, Long performedByUserId) {
        User user = getOrThrow(userId);
        String oldValueJson = AuditSnapshotSerializer.toJson(teamSnapshot(user.getTeam() != null ? user.getTeam().getId() : null));
        Team team = teamService.getOrThrow(teamId);
        user.setTeam(team);
        User saved = AuditExecutor.execute(
                auditLogService,
                cz.cvut.fit.budget_app.entity.AuditLog.OperationType.USER_TEAM_ASSIGNED,
                cz.cvut.fit.budget_app.entity.AuditLog.EntityType.USER,
                performedByUserId,
                oldValueJson,
                () -> userRepository.save(user),
                User::getId,
                u -> AuditSnapshotSerializer.toJson(teamSnapshot(u.getTeam() != null ? u.getTeam().getId() : null))
        );
        return UserResponse.from(saved);
    }

    @Transactional
    public UserResponse unassignTeam(Long userId, Long performedByUserId) {
        User user = getOrThrow(userId);
        String oldValueJson = AuditSnapshotSerializer.toJson(teamSnapshot(user.getTeam() != null ? user.getTeam().getId() : null));
        user.setTeam(null);
        User saved = AuditExecutor.execute(
                auditLogService,
                cz.cvut.fit.budget_app.entity.AuditLog.OperationType.USER_TEAM_UNASSIGNED,
                cz.cvut.fit.budget_app.entity.AuditLog.EntityType.USER,
                performedByUserId,
                oldValueJson,
                () -> userRepository.save(user),
                User::getId,
                u -> AuditSnapshotSerializer.toJson(teamSnapshot(null))
        );
        return UserResponse.from(saved);
    }

    private Map<String, Object> teamSnapshot(Long teamId) {
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("teamId", teamId);
        return snapshot;
    }

    public User getOrThrow(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", id));
    }
}
