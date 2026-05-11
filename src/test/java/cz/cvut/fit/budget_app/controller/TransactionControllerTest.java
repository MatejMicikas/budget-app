package cz.cvut.fit.budget_app.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import cz.cvut.fit.budget_app.config.TestMethodSecurityConfiguration;
import cz.cvut.fit.budget_app.dto.request.CreateTransactionRequest;
import cz.cvut.fit.budget_app.dto.request.UpdateTransactionRequest;
import cz.cvut.fit.budget_app.dto.response.TransactionResponse;
import cz.cvut.fit.budget_app.entity.Transaction;
import cz.cvut.fit.budget_app.entity.User;
import cz.cvut.fit.budget_app.exception.SeasonClosedException;
import cz.cvut.fit.budget_app.security.JwtAuthenticationFilter;
import cz.cvut.fit.budget_app.security.JwtTokenProvider;
import cz.cvut.fit.budget_app.security.UserDetailsServiceImpl;
import cz.cvut.fit.budget_app.security.UserPrincipal;
import cz.cvut.fit.budget_app.service.TransactionService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TransactionController.class)
@Import({JwtAuthenticationFilter.class, TestMethodSecurityConfiguration.class})
class TransactionControllerTest {

    @Autowired MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    @MockitoBean TransactionService transactionService;
    @MockitoBean JwtTokenProvider jwtTokenProvider;
    @MockitoBean UserDetailsServiceImpl userDetailsService;

    @Test
    void findBySeason_asMember_returns403() throws Exception {
        when(transactionService.findBySeasonWithFilters(1L, null, null, User.Role.MEMBER, null)).thenReturn(List.of());

        mockMvc.perform(get("/api/transactions")
                        .param("seasonId", "1")
                        .with(user(principalFor(User.Role.MEMBER))))
                .andExpect(status().isForbidden());
    }

    @Test
    void findBySeason_unauthenticated_returns401() throws Exception {
        mockMvc.perform(get("/api/transactions").param("seasonId", "1"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void findByFundingSource_asMember_returns403() throws Exception {
        when(transactionService.findByFundingSourceId(5L, User.Role.MEMBER, null)).thenReturn(List.of());

        mockMvc.perform(get("/api/transactions")
                        .param("fundingSourceId", "5")
                        .with(user(principalFor(User.Role.MEMBER))))
                .andExpect(status().isForbidden());
    }

    @Test
    void findBySeason_asTeamLeader_returns200() throws Exception {
        when(transactionService.findBySeasonWithFilters(1L, null, null, User.Role.TEAM_LEADER, 1L)).thenReturn(List.of());

        mockMvc.perform(get("/api/transactions")
                        .param("seasonId", "1")
                        .with(user(principalFor(User.Role.TEAM_LEADER))))
                .andExpect(status().isOk());
    }

    @Test
    void findBySeasonTypeAndBudgetItem_asTeamLeader_returns200() throws Exception {
        when(transactionService.findBySeasonWithFilters(1L, Transaction.TransactionType.ACTUAL, 5L, User.Role.TEAM_LEADER, 1L))
                .thenReturn(List.of());

        mockMvc.perform(get("/api/transactions")
                        .param("seasonId", "1")
                        .param("type", "ACTUAL")
                        .param("budgetItemId", "5")
                        .with(user(principalFor(User.Role.TEAM_LEADER))))
                .andExpect(status().isOk());
    }

    @Test
    void find_withoutAnyFilter_returns400() throws Exception {
        mockMvc.perform(get("/api/transactions")
                        .with(user(principalFor(User.Role.TREASURER))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void create_asTeamLeader_planned_returns201() throws Exception {
        CreateTransactionRequest req = buildRequest(Transaction.TransactionType.PLANNED);
        when(transactionService.create(any(), any(), any(), any())).thenReturn(buildResponse());

        mockMvc.perform(post("/api/transactions")
                        .with(csrf())
                        .with(user(principalFor(User.Role.TEAM_LEADER)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.type").value("PLANNED"));
    }

    @Test
    void create_asTeamLeader_actual_returns403() throws Exception {
        CreateTransactionRequest req = buildRequest(Transaction.TransactionType.ACTUAL);

        mockMvc.perform(post("/api/transactions")
                        .with(csrf())
                        .with(user(principalFor(User.Role.TEAM_LEADER)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isForbidden());

        verify(transactionService, never()).create(any(), any(), any(), any());
    }

    @Test
    void create_asMember_returns403() throws Exception {
        mockMvc.perform(post("/api/transactions")
                        .with(csrf())
                        .with(user(principalFor(User.Role.MEMBER)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildRequest(Transaction.TransactionType.PLANNED))))
                .andExpect(status().isForbidden());
    }

    @Test
    void create_invalidBody_returns400() throws Exception {
        CreateTransactionRequest req = buildRequest(Transaction.TransactionType.PLANNED);
        req.setAmount(BigDecimal.ZERO);

        mockMvc.perform(post("/api/transactions")
                        .with(csrf())
                        .with(user(principalFor(User.Role.TREASURER)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());

        verify(transactionService, never()).create(any(), any(), any(), any());
    }

    @Test
    void cancel_asTreasurer_returns200() throws Exception {
        when(transactionService.cancel(eq(10L), any(), eq(1L))).thenReturn(buildResponse());

        mockMvc.perform(post("/api/transactions/10/cancel")
                        .with(csrf())
                        .with(user(principalFor(User.Role.TREASURER)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk());

        verify(transactionService).cancel(eq(10L), any(), eq(1L));
    }

    @Test
    void update_asTreasurer_returns200() throws Exception {
        UpdateTransactionRequest req = buildUpdateRequest();
        when(transactionService.update(eq(10L), any(), eq(1L))).thenReturn(buildResponse());

        mockMvc.perform(put("/api/transactions/10")
                        .with(csrf())
                        .with(user(principalFor(User.Role.TREASURER)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk());
    }

    @Test
    void update_asTeamLeader_returns403() throws Exception {
        mockMvc.perform(put("/api/transactions/10")
                        .with(csrf())
                        .with(user(principalFor(User.Role.TEAM_LEADER)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildUpdateRequest())))
                .andExpect(status().isForbidden());
    }

    @Test
    void update_closedSeason_returns409() throws Exception {
        doThrow(new SeasonClosedException(1L)).when(transactionService).update(eq(10L), any(), eq(1L));

        mockMvc.perform(put("/api/transactions/10")
                        .with(csrf())
                        .with(user(principalFor(User.Role.TREASURER)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildUpdateRequest())))
                .andExpect(status().isConflict());
    }

    @Test
    void approve_asTreasurer_returns200() throws Exception {
        when(transactionService.approve(10L, 1L)).thenReturn(buildResponse());

        mockMvc.perform(post("/api/transactions/10/approve")
                        .with(csrf())
                        .with(user(principalFor(User.Role.TREASURER))))
                .andExpect(status().isOk());
    }

    @Test
    void approve_asTeamLeader_returns403() throws Exception {
        mockMvc.perform(post("/api/transactions/10/approve")
                        .with(csrf())
                        .with(user(principalFor(User.Role.TEAM_LEADER))))
                .andExpect(status().isForbidden());
    }

    @Test
    void reject_asTreasurer_returns200() throws Exception {
        when(transactionService.reject(10L, 1L)).thenReturn(buildResponse());

        mockMvc.perform(post("/api/transactions/10/reject")
                        .with(csrf())
                        .with(user(principalFor(User.Role.TREASURER))))
                .andExpect(status().isOk());
    }

    @Test
    void reject_asTeamLeader_returns403() throws Exception {
        mockMvc.perform(post("/api/transactions/10/reject")
                        .with(csrf())
                        .with(user(principalFor(User.Role.TEAM_LEADER))))
                .andExpect(status().isForbidden());
    }

    @Test
    void cancel_asTeamLeader_returns403() throws Exception {
        mockMvc.perform(post("/api/transactions/10/cancel")
                        .with(csrf())
                        .with(user(principalFor(User.Role.TEAM_LEADER)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void delete_asTreasurer_returns204() throws Exception {
        mockMvc.perform(delete("/api/transactions/10")
                        .with(csrf())
                        .with(user(principalFor(User.Role.TREASURER))))
                .andExpect(status().isNoContent());

        verify(transactionService).delete(10L, 1L);
    }

    @Test
    void delete_asTeamLeader_returns403() throws Exception {
        mockMvc.perform(delete("/api/transactions/10")
                        .with(csrf())
                        .with(user(principalFor(User.Role.TEAM_LEADER))))
                .andExpect(status().isForbidden());

        verify(transactionService, never()).delete(any(), any());
    }

    private UserPrincipal principalFor(User.Role role) {
        User user = new User();
        user.setId(1L);
        user.setUsername("testuser");
        user.setPasswordHash("hash");
        user.setRole(role);
        return new UserPrincipal(user);
    }

    private CreateTransactionRequest buildRequest(Transaction.TransactionType type) {
        CreateTransactionRequest r = new CreateTransactionRequest();
        r.setDate(LocalDate.of(2024, 9, 2));
        r.setAmount(new BigDecimal("1000"));
        r.setType(type);
        r.setDirection(Transaction.Direction.EXPENSE);
        r.setDescription("Hall rent");
        r.setSeasonId(1L);
        r.setBudgetItemId(2L);
        return r;
    }

    private TransactionResponse buildResponse() {
        TransactionResponse r = new TransactionResponse();
        r.setId(10L);
        r.setDate(LocalDate.of(2024, 9, 2));
        r.setAmount(new BigDecimal("1000"));
        r.setType(Transaction.TransactionType.PLANNED);
        r.setDirection(Transaction.Direction.EXPENSE);
        r.setDescription("Hall rent");
        r.setBudgetItemId(2L);
        r.setSeasonId(1L);
        return r;
    }

    private UpdateTransactionRequest buildUpdateRequest() {
        UpdateTransactionRequest r = new UpdateTransactionRequest();
        r.setDate(LocalDate.of(2024, 9, 2));
        r.setAmount(new BigDecimal("1000"));
        r.setDescription("Hall rent updated");
        return r;
    }
}
