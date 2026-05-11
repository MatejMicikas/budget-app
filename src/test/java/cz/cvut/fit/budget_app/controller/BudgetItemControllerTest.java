package cz.cvut.fit.budget_app.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import cz.cvut.fit.budget_app.dto.request.CreateBudgetItemRequest;
import cz.cvut.fit.budget_app.dto.request.UpdateBudgetItemRequest;
import cz.cvut.fit.budget_app.dto.response.BudgetItemResponse;
import cz.cvut.fit.budget_app.dto.response.BudgetSummaryResponse;
import cz.cvut.fit.budget_app.entity.BudgetItem;
import cz.cvut.fit.budget_app.entity.Season;
import cz.cvut.fit.budget_app.entity.Transaction;
import cz.cvut.fit.budget_app.entity.User;
import cz.cvut.fit.budget_app.exception.SeasonClosedException;
import cz.cvut.fit.budget_app.security.JwtAuthenticationFilter;
import cz.cvut.fit.budget_app.security.JwtTokenProvider;
import cz.cvut.fit.budget_app.security.UserDetailsServiceImpl;
import cz.cvut.fit.budget_app.security.UserPrincipal;
import cz.cvut.fit.budget_app.service.BudgetItemService;
import cz.cvut.fit.budget_app.service.BudgetSummaryService;
import cz.cvut.fit.budget_app.service.SeasonService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(BudgetItemController.class)
@Import({JwtAuthenticationFilter.class})
class BudgetItemControllerTest {

    @Autowired MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @MockitoBean BudgetItemService budgetItemService;
    @MockitoBean BudgetSummaryService budgetSummaryService;
    @MockitoBean SeasonService seasonService;
    @MockitoBean JwtTokenProvider jwtTokenProvider;
    @MockitoBean UserDetailsServiceImpl userDetailsService;

    @Test
    void findBySeason_asMember_returns403() throws Exception {
        when(budgetItemService.findBySeasonId(1L, User.Role.MEMBER, null)).thenReturn(List.of());

        mockMvc.perform(get("/api/budget-items")
                        .param("seasonId", "1")
                        .with(user(principalFor(User.Role.MEMBER))))
                .andExpect(status().isForbidden());
    }

    @Test
    void findBySeason_asTeamLeader_returns200() throws Exception {
        when(budgetItemService.findBySeasonId(1L, User.Role.TEAM_LEADER, 1L)).thenReturn(List.of());

        mockMvc.perform(get("/api/budget-items")
                        .param("seasonId", "1")
                        .with(user(principalFor(User.Role.TEAM_LEADER))))
                .andExpect(status().isOk());
    }

    @Test
    void summary_withFilters_asTeamLeader_returns200() throws Exception {
        when(budgetSummaryService.getSeasonSummaryForTeam(1L, 1L, BudgetItem.ItemType.EXPENSE, Transaction.TransactionType.ACTUAL))
                .thenReturn(new BudgetSummaryResponse());

        mockMvc.perform(get("/api/budget-items/summary")
                        .param("seasonId", "1")
                        .param("itemType", "EXPENSE")
                        .param("transactionType", "ACTUAL")
                        .with(user(principalFor(User.Role.TEAM_LEADER))))
                .andExpect(status().isOk());
    }

    @Test
    void summary_asMember_returns200() throws Exception {
        when(budgetSummaryService.getSeasonSummary(1L, null, null))
                .thenReturn(new BudgetSummaryResponse());
        Season season = new Season();
        season.setId(1L);
        season.setMemberSummaryVisible(true);
        when(seasonService.getOrThrow(1L)).thenReturn(season);

        mockMvc.perform(get("/api/budget-items/summary")
                        .param("seasonId", "1")
                        .with(user(principalFor(User.Role.MEMBER))))
                .andExpect(status().isOk());
    }

    @Test
    void summary_asMember_whenNotVisible_returns403() throws Exception {
        Season season = new Season();
        season.setId(1L);
        season.setMemberSummaryVisible(false);
        when(seasonService.getOrThrow(1L)).thenReturn(season);

        mockMvc.perform(get("/api/budget-items/summary")
                        .param("seasonId", "1")
                        .with(user(principalFor(User.Role.MEMBER))))
                .andExpect(status().isForbidden());
    }

    @Test
    void findBySeason_unauthenticated_returns401() throws Exception {
        mockMvc.perform(get("/api/budget-items").param("seasonId", "1"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void create_asTreasurer_returns201() throws Exception {
        when(budgetItemService.create(any(), any())).thenReturn(buildResponse());

        mockMvc.perform(post("/api/budget-items")
                        .with(user(principalFor(User.Role.TREASURER)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildRequest())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Hala"))
                .andExpect(jsonPath("$.balance").value(10000));
    }

    @Test
    void create_asMember_returns403() throws Exception {
        mockMvc.perform(post("/api/budget-items")
                        .with(user(principalFor(User.Role.MEMBER)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildRequest())))
                .andExpect(status().isForbidden());
    }

    @Test
    void create_invalidBody_returns400() throws Exception {
        CreateBudgetItemRequest req = buildRequest();
        req.setName(" ");

        mockMvc.perform(post("/api/budget-items")
                        .with(user(principalFor(User.Role.ADMIN)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());

        verify(budgetItemService, never()).create(any(), any());
    }

    @Test
    void update_asTreasurer_returns200() throws Exception {
        when(budgetItemService.update(eq(10L), any(), eq(1L))).thenReturn(buildResponse());

        mockMvc.perform(put("/api/budget-items/10")
                        .with(user(principalFor(User.Role.TREASURER)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildUpdateRequest())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(10));
    }

    @Test
    void update_asMember_returns403() throws Exception {
        mockMvc.perform(put("/api/budget-items/10")
                        .with(user(principalFor(User.Role.MEMBER)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildUpdateRequest())))
                .andExpect(status().isForbidden());
    }

    @Test
    void update_closedSeason_returns409() throws Exception {
        doThrow(new SeasonClosedException(1L)).when(budgetItemService).update(eq(10L), any(), eq(1L));

        mockMvc.perform(put("/api/budget-items/10")
                        .with(user(principalFor(User.Role.TREASURER)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildUpdateRequest())))
                .andExpect(status().isConflict());
    }

    @Test
    void delete_asTreasurer_returns204() throws Exception {
        mockMvc.perform(delete("/api/budget-items/10")
                        .with(user(principalFor(User.Role.TREASURER))))
                .andExpect(status().isNoContent());

        verify(budgetItemService).delete(eq(10L), eq(1L));
    }

    @Test
    void delete_asTeamLeader_returns403() throws Exception {
        mockMvc.perform(delete("/api/budget-items/10")
                        .with(user(principalFor(User.Role.TEAM_LEADER))))
                .andExpect(status().isForbidden());
    }

    private UserPrincipal principalFor(User.Role role) {
        User user = new User();
        user.setId(1L);
        user.setUsername("testuser");
        user.setPasswordHash("hash");
        user.setRole(role);
        return new UserPrincipal(user);
    }

    private CreateBudgetItemRequest buildRequest() {
        CreateBudgetItemRequest r = new CreateBudgetItemRequest();
        r.setName("Hala");
        r.setType(BudgetItem.ItemType.EXPENSE);
        r.setPlannedAmount(new BigDecimal("10000"));
        r.setSeasonId(1L);
        r.setFundingSourceId(2L);
        return r;
    }

    private BudgetItemResponse buildResponse() {
        BudgetItemResponse r = new BudgetItemResponse();
        r.setId(10L);
        r.setName("Hala");
        r.setType(BudgetItem.ItemType.EXPENSE);
        r.setPlannedAmount(new BigDecimal("10000"));
        r.setActualAmount(BigDecimal.ZERO);
        r.setBalance(new BigDecimal("10000"));
        r.setSeasonId(1L);
        r.setFundingSourceId(2L);
        return r;
    }

    private UpdateBudgetItemRequest buildUpdateRequest() {
        UpdateBudgetItemRequest r = new UpdateBudgetItemRequest();
        r.setName("Hala Updated");
        r.setType(BudgetItem.ItemType.EXPENSE);
        r.setPlannedAmount(new BigDecimal("11000"));
        r.setFundingSourceId(2L);
        return r;
    }
}
