package cz.cvut.fit.budget_app.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import cz.cvut.fit.budget_app.config.TestMethodSecurityConfiguration;
import cz.cvut.fit.budget_app.dto.request.CreateFundingSourceRequest;
import cz.cvut.fit.budget_app.dto.request.UpdateFundingSourceRequest;
import cz.cvut.fit.budget_app.dto.response.FundingSourceResponse;
import cz.cvut.fit.budget_app.entity.FundingSource;
import cz.cvut.fit.budget_app.entity.User;
import cz.cvut.fit.budget_app.exception.SeasonClosedException;
import cz.cvut.fit.budget_app.security.JwtAuthenticationFilter;
import cz.cvut.fit.budget_app.security.JwtTokenProvider;
import cz.cvut.fit.budget_app.security.UserDetailsServiceImpl;
import cz.cvut.fit.budget_app.security.UserPrincipal;
import cz.cvut.fit.budget_app.service.FundingSourceService;
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
import static org.mockito.Mockito.doThrow;
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

@WebMvcTest(FundingSourceController.class)
@Import({JwtAuthenticationFilter.class, TestMethodSecurityConfiguration.class})
class FundingSourceControllerTest {

    @Autowired MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @MockitoBean FundingSourceService fundingSourceService;
    @MockitoBean JwtTokenProvider jwtTokenProvider;
    @MockitoBean UserDetailsServiceImpl userDetailsService;

    @Test
    void create_asTreasurer_returns201() throws Exception {
        when(fundingSourceService.create(any(), any())).thenReturn(buildResponse());

        mockMvc.perform(post("/api/funding-sources")
                        .with(csrf())
                        .with(user(principalFor(User.Role.TREASURER)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildCreateRequest())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Grant Youth"));
    }

    @Test
    void create_nullAllocatedAmount_passesValidation_returns201() throws Exception {
        when(fundingSourceService.create(any(), any())).thenReturn(buildResponse());

        String json = """
                {"name":"Open grant","type":"PUBLIC_GRANT","seasonId":1,"allocatedAmount":null}
                """;

        mockMvc.perform(post("/api/funding-sources")
                        .with(csrf())
                        .with(user(principalFor(User.Role.TREASURER)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isCreated());
    }

    @Test
    void create_omittedAllocatedAmount_passesValidation_returns201() throws Exception {
        when(fundingSourceService.create(any(), any())).thenReturn(buildResponse());

        String json = """
                {"name":"Open grant","type":"PUBLIC_GRANT","seasonId":1}
                """;

        mockMvc.perform(post("/api/funding-sources")
                        .with(csrf())
                        .with(user(principalFor(User.Role.TREASURER)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isCreated());
    }

    @Test
    void update_asAdmin_returns200() throws Exception {
        when(fundingSourceService.update(eq(7L), any(), any())).thenReturn(buildResponse());

        mockMvc.perform(put("/api/funding-sources/7")
                        .with(csrf())
                        .with(user(principalFor(User.Role.ADMIN)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildUpdateRequest())))
                .andExpect(status().isOk());
    }

    @Test
    void update_closedSeason_returns409() throws Exception {
        doThrow(new SeasonClosedException(1L)).when(fundingSourceService).update(eq(7L), any(), eq(1L));

        mockMvc.perform(put("/api/funding-sources/7")
                        .with(csrf())
                        .with(user(principalFor(User.Role.TREASURER)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildUpdateRequest())))
                .andExpect(status().isConflict());
    }

    @Test
    void delete_asTreasurer_returns204() throws Exception {
        mockMvc.perform(delete("/api/funding-sources/7")
                        .with(csrf())
                        .with(user(principalFor(User.Role.TREASURER))))
                .andExpect(status().isNoContent());

        verify(fundingSourceService).delete(7L, 1L);
    }

    @Test
    void delete_closedSeason_returns409() throws Exception {
        doThrow(new SeasonClosedException(1L)).when(fundingSourceService).delete(7L, 1L);

        mockMvc.perform(delete("/api/funding-sources/7")
                        .with(csrf())
                        .with(user(principalFor(User.Role.TREASURER))))
                .andExpect(status().isConflict());
    }

    @Test
    void spendingReport_asMember_returns403() throws Exception {
        when(fundingSourceService.findById(7L, User.Role.MEMBER, null)).thenReturn(buildResponse());

        mockMvc.perform(get("/api/funding-sources/7/spending-report")
                        .with(user(principalFor(User.Role.MEMBER))))
                .andExpect(status().isForbidden());
    }

    @Test
    void create_asMember_returns403() throws Exception {
        mockMvc.perform(post("/api/funding-sources")
                        .with(csrf())
                        .with(user(principalFor(User.Role.MEMBER)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildCreateRequest())))
                .andExpect(status().isForbidden());
    }

    @Test
    void findBySeason_asMember_returns403() throws Exception {
        when(fundingSourceService.findBySeasonId(1L, User.Role.MEMBER, null)).thenReturn(List.of(buildResponse()));

        mockMvc.perform(get("/api/funding-sources")
                        .param("seasonId", "1")
                        .with(user(principalFor(User.Role.MEMBER))))
                .andExpect(status().isForbidden());
    }

    @Test
    void findBySeason_asTeamLeader_returns200() throws Exception {
        when(fundingSourceService.findBySeasonId(1L, User.Role.TEAM_LEADER, 1L)).thenReturn(List.of(buildResponse()));

        mockMvc.perform(get("/api/funding-sources")
                        .param("seasonId", "1")
                        .with(user(principalFor(User.Role.TEAM_LEADER))))
                .andExpect(status().isOk());
    }

    private UserPrincipal principalFor(User.Role role) {
        User user = new User();
        user.setId(1L);
        user.setUsername("testuser");
        user.setPasswordHash("hash");
        user.setRole(role);
        return new UserPrincipal(user);
    }

    private CreateFundingSourceRequest buildCreateRequest() {
        CreateFundingSourceRequest r = new CreateFundingSourceRequest();
        r.setName("Grant Youth");
        r.setType(FundingSource.FundingType.PUBLIC_GRANT);
        r.setAllocatedAmount(new BigDecimal("15000"));
        r.setSeasonId(1L);
        return r;
    }

    private UpdateFundingSourceRequest buildUpdateRequest() {
        UpdateFundingSourceRequest r = new UpdateFundingSourceRequest();
        r.setName("Grant Youth Updated");
        r.setType(FundingSource.FundingType.PUBLIC_GRANT);
        r.setAllocatedAmount(new BigDecimal("16000"));
        return r;
    }

    private FundingSourceResponse buildResponse() {
        FundingSourceResponse r = new FundingSourceResponse();
        r.setId(7L);
        r.setName("Grant Youth");
        r.setType(FundingSource.FundingType.PUBLIC_GRANT);
        r.setAllocatedAmount(new BigDecimal("10000"));
        r.setActualSpending(new BigDecimal("12000"));
        r.setLimitExceeded(true);
        r.setSeasonId(1L);
        return r;
    }
}
