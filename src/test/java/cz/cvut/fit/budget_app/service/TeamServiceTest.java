package cz.cvut.fit.budget_app.service;

import cz.cvut.fit.budget_app.entity.Team;
import cz.cvut.fit.budget_app.repository.BudgetItemRepository;
import cz.cvut.fit.budget_app.repository.TeamRepository;
import cz.cvut.fit.budget_app.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TeamServiceTest {

    @Mock TeamRepository teamRepository;
    @Mock UserRepository userRepository;
    @Mock BudgetItemRepository budgetItemRepository;
    @Mock AuditLogService auditLogService;

    @InjectMocks TeamService teamService;

    @Test
    void delete_noAssignments_deletesAndAudits() {
        Team team = new Team();
        team.setId(10L);
        team.setName("Alpha");
        when(teamRepository.findById(10L)).thenReturn(Optional.of(team));
        when(userRepository.countByTeam_Id(10L)).thenReturn(0L);
        when(budgetItemRepository.countByTeam_Id(10L)).thenReturn(0L);

        teamService.delete(10L, 99L);

        verify(teamRepository).delete(team);
        verify(auditLogService).logTeamDeleted(eq(10L), eq(99L), any());
    }

    @Test
    void delete_withAssignedUsers_throwsAndDoesNotDelete() {
        Team team = new Team();
        team.setId(10L);
        team.setName("Alpha");
        when(teamRepository.findById(10L)).thenReturn(Optional.of(team));
        when(userRepository.countByTeam_Id(10L)).thenReturn(2L);
        when(budgetItemRepository.countByTeam_Id(10L)).thenReturn(0L);

        assertThatThrownBy(() -> teamService.delete(10L, 99L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Cannot delete team")
                .hasMessageContaining("2 user");

        verify(teamRepository, never()).delete(any());
        verify(auditLogService, never()).logTeamDeleted(any(), any(), any());
    }

    @Test
    void delete_withAssignedBudgetItems_throwsAndDoesNotDelete() {
        Team team = new Team();
        team.setId(10L);
        team.setName("Alpha");
        when(teamRepository.findById(10L)).thenReturn(Optional.of(team));
        when(userRepository.countByTeam_Id(10L)).thenReturn(0L);
        when(budgetItemRepository.countByTeam_Id(10L)).thenReturn(3L);

        assertThatThrownBy(() -> teamService.delete(10L, 99L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("3 budget item");

        verify(teamRepository, never()).delete(any());
        verify(auditLogService, never()).logTeamDeleted(any(), any(), any());
    }
}
