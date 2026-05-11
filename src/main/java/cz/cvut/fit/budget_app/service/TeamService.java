package cz.cvut.fit.budget_app.service;

import cz.cvut.fit.budget_app.dto.request.CreateTeamRequest;
import cz.cvut.fit.budget_app.dto.request.UpdateTeamRequest;
import cz.cvut.fit.budget_app.dto.response.TeamResponse;
import cz.cvut.fit.budget_app.entity.Team;
import cz.cvut.fit.budget_app.exception.ResourceNotFoundException;
import cz.cvut.fit.budget_app.repository.BudgetItemRepository;
import cz.cvut.fit.budget_app.repository.TeamRepository;
import cz.cvut.fit.budget_app.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class TeamService {

    private final TeamRepository teamRepository;
    private final UserRepository userRepository;
    private final BudgetItemRepository budgetItemRepository;
    private final AuditLogService auditLogService;

    @Transactional
    public TeamResponse create(CreateTeamRequest request, Long performedByUserId) {
        if (teamRepository.existsByNameIgnoreCase(request.getName())) {
            throw new IllegalArgumentException("Team '" + request.getName() + "' already exists");
        }
        Team team = new Team();
        team.setName(request.getName());
        Team saved = teamRepository.save(team);
        String newValueJson = AuditSnapshotSerializer.toJson(Map.of("name", saved.getName()));
        auditLogService.logTeamCreated(saved.getId(), performedByUserId, newValueJson);
        return TeamResponse.from(saved);
    }

    @Transactional
    public TeamResponse update(Long id, UpdateTeamRequest request, Long performedByUserId) {
        Team team = getOrThrow(id);
        if (!team.getName().equalsIgnoreCase(request.getName())
                && teamRepository.existsByNameIgnoreCase(request.getName())) {
            throw new IllegalArgumentException("Team '" + request.getName() + "' already exists");
        }
        String oldValueJson = AuditSnapshotSerializer.toJson(Map.of("name", team.getName()));
        team.setName(request.getName());
        Team saved = teamRepository.save(team);
        String newValueJson = AuditSnapshotSerializer.toJson(Map.of("name", saved.getName()));
        auditLogService.logTeamUpdated(saved.getId(), performedByUserId, oldValueJson, newValueJson);
        return TeamResponse.from(saved);
    }

    @Transactional(readOnly = true)
    public TeamResponse findById(Long id) {
        return TeamResponse.from(getOrThrow(id));
    }

    @Transactional(readOnly = true)
    public List<TeamResponse> findAll() {
        return teamRepository.findAll().stream()
                .map(TeamResponse::from)
                .toList();
    }

    @Transactional
    public void delete(Long id, Long performedByUserId) {
        Team team = getOrThrow(id);
        long assignedUsers = userRepository.countByTeam_Id(id);
        long assignedBudgetItems = budgetItemRepository.countByTeam_Id(id);
        if (assignedUsers > 0 || assignedBudgetItems > 0) {
            throw new IllegalStateException(
                    "Cannot delete team while it is still assigned to "
                            + assignedUsers + " user(s) and "
                            + assignedBudgetItems + " budget item(s). "
                            + "Reassign or remove those assignments first.");
        }
        String oldValueJson = AuditSnapshotSerializer.toJson(Map.of("name", team.getName()));
        teamRepository.delete(team);
        auditLogService.logTeamDeleted(id, performedByUserId, oldValueJson);
    }

    public Team getOrThrow(Long id) {
        return teamRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Team", id));
    }
}
