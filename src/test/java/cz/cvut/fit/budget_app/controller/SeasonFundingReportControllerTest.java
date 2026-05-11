package cz.cvut.fit.budget_app.controller;

import cz.cvut.fit.budget_app.dto.response.SeasonFundingSpendingReportResponse;
import cz.cvut.fit.budget_app.entity.User;
import cz.cvut.fit.budget_app.security.JwtAuthenticationFilter;
import cz.cvut.fit.budget_app.security.JwtTokenProvider;
import cz.cvut.fit.budget_app.security.UserDetailsServiceImpl;
import cz.cvut.fit.budget_app.security.UserPrincipal;
import cz.cvut.fit.budget_app.service.FundingSourceService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(SeasonFundingReportController.class)
@Import({JwtAuthenticationFilter.class})
class SeasonFundingReportControllerTest {

    @Autowired MockMvc mockMvc;

    @MockitoBean FundingSourceService fundingSourceService;
    @MockitoBean JwtTokenProvider jwtTokenProvider;
    @MockitoBean UserDetailsServiceImpl userDetailsService;

    @Test
    void report_asTreasurer_returns200() throws Exception {
        SeasonFundingSpendingReportResponse response = new SeasonFundingSpendingReportResponse();
        response.setSeasonId(1L);
        response.setSourceCount(2);
        response.setTotalAllocatedAmount(new BigDecimal("10000.00"));
        response.setTotalActualSpending(new BigDecimal("7000.00"));
        response.setTotalRemainingAmount(new BigDecimal("3000.00"));
        when(fundingSourceService.getSeasonSpendingReport(1L, User.Role.TREASURER, null)).thenReturn(response);

        mockMvc.perform(get("/api/seasons/1/funding-sources/spending-report")
                        .with(user(principalFor(User.Role.TREASURER))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.seasonId").value(1))
                .andExpect(jsonPath("$.sourceCount").value(2));

        verify(fundingSourceService).getSeasonSpendingReport(1L, User.Role.TREASURER, null);
    }

    @Test
    void report_asTeamLeader_returns200AndUsesTeamScope() throws Exception {
        SeasonFundingSpendingReportResponse response = new SeasonFundingSpendingReportResponse();
        response.setSeasonId(1L);
        response.setSourceCount(1);
        when(fundingSourceService.getSeasonSpendingReport(1L, User.Role.TEAM_LEADER, 3L)).thenReturn(response);

        mockMvc.perform(get("/api/seasons/1/funding-sources/spending-report")
                        .with(user(principalFor(User.Role.TEAM_LEADER, 3L))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sourceCount").value(1));

        verify(fundingSourceService).getSeasonSpendingReport(1L, User.Role.TEAM_LEADER, 3L);
    }

    private UserPrincipal principalFor(User.Role role) {
        return principalFor(role, null);
    }

    private UserPrincipal principalFor(User.Role role, Long teamId) {
        User user = new User();
        user.setId(1L);
        user.setUsername("testuser");
        user.setPasswordHash("hash");
        user.setRole(role);
        if (teamId != null) {
            cz.cvut.fit.budget_app.entity.Team team = new cz.cvut.fit.budget_app.entity.Team();
            team.setId(teamId);
            user.setTeam(team);
        }
        return new UserPrincipal(user);
    }
}
