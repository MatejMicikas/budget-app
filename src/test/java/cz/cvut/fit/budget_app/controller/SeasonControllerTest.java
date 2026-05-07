package cz.cvut.fit.budget_app.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import cz.cvut.fit.budget_app.dto.request.CreateSeasonRequest;
import cz.cvut.fit.budget_app.dto.response.SeasonResponse;
import cz.cvut.fit.budget_app.entity.Season;
import cz.cvut.fit.budget_app.entity.User;
import cz.cvut.fit.budget_app.exception.SeasonClosedException;
import cz.cvut.fit.budget_app.security.JwtAuthenticationFilter;
import cz.cvut.fit.budget_app.security.JwtTokenProvider;
import cz.cvut.fit.budget_app.security.UserDetailsServiceImpl;
import cz.cvut.fit.budget_app.security.UserPrincipal;
import cz.cvut.fit.budget_app.service.SeasonService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(SeasonController.class)
@Import({JwtAuthenticationFilter.class})
class SeasonControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    @MockitoBean SeasonService seasonService;
    @MockitoBean JwtTokenProvider jwtTokenProvider;
    @MockitoBean UserDetailsServiceImpl userDetailsService;

    @Test
    void getAll_asAnyAuthenticatedUser_returns200() throws Exception {
        when(seasonService.findAll()).thenReturn(List.of());

        mockMvc.perform(get("/api/seasons")
                        .with(user(principalFor(User.Role.MEMBER))))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON));
    }

    @Test
    void getAll_unauthenticated_returns401() throws Exception {
        mockMvc.perform(get("/api/seasons"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void create_asTreasurer_returns201() throws Exception {
        CreateSeasonRequest req = buildCreateRequest();
        SeasonResponse resp = buildSeasonResponse();
        when(seasonService.create(any())).thenReturn(resp);

        mockMvc.perform(post("/api/seasons")
                        .with(user(principalFor(User.Role.TREASURER)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("2024/2025"));
    }

    @Test
    void create_asAdmin_returns201() throws Exception {
        when(seasonService.create(any())).thenReturn(buildSeasonResponse());

        mockMvc.perform(post("/api/seasons")
                        .with(user(principalFor(User.Role.ADMIN)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildCreateRequest())))
                .andExpect(status().isCreated());
    }

    @Test
    void create_asMember_returns403() throws Exception {
        mockMvc.perform(post("/api/seasons")
                        .with(user(principalFor(User.Role.MEMBER)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildCreateRequest())))
                .andExpect(status().isForbidden());
    }

    @Test
    void create_asTeamLeader_returns403() throws Exception {
        mockMvc.perform(post("/api/seasons")
                        .with(user(principalFor(User.Role.TEAM_LEADER)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildCreateRequest())))
                .andExpect(status().isForbidden());
    }

    @Test
    void create_unauthenticated_returns401() throws Exception {
        mockMvc.perform(post("/api/seasons")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildCreateRequest())))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void create_invalidBody_returns400() throws Exception {
        CreateSeasonRequest invalid = buildCreateRequest();
        invalid.setName(" ");

        mockMvc.perform(post("/api/seasons")
                        .with(user(principalFor(User.Role.ADMIN)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalid)))
                .andExpect(status().isBadRequest());

        verify(seasonService, never()).create(any());
    }

    @Test
    void getById_asAnyAuthenticatedUser_returns200() throws Exception {
        when(seasonService.findById(1L)).thenReturn(buildSeasonResponse());

        mockMvc.perform(get("/api/seasons/1")
                        .with(user(principalFor(User.Role.MEMBER))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L));
    }

    @Test
    void getById_unauthenticated_returns401() throws Exception {
        mockMvc.perform(get("/api/seasons/1"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void close_asAdmin_returns200() throws Exception {
        when(seasonService.close(anyLong(), anyLong())).thenReturn(buildSeasonResponse());

        mockMvc.perform(post("/api/seasons/1/close")
                        .with(user(principalFor(User.Role.ADMIN))))
                .andExpect(status().isOk());

        verify(seasonService).close(eq(1L), eq(1L));
    }

    @Test
    void close_asTreasurer_returns403() throws Exception {
        mockMvc.perform(post("/api/seasons/1/close")
                        .with(user(principalFor(User.Role.TREASURER))))
                .andExpect(status().isForbidden());
    }

    @Test
    void close_unauthenticated_returns401() throws Exception {
        mockMvc.perform(post("/api/seasons/1/close"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void close_alreadyClosed_returns409() throws Exception {
        when(seasonService.close(1L, 1L)).thenThrow(new SeasonClosedException(1L));

        mockMvc.perform(post("/api/seasons/1/close")
                        .with(user(principalFor(User.Role.ADMIN))))
                .andExpect(status().isConflict());
    }

    // ---- helpers ----

    private UserPrincipal principalFor(User.Role role) {
        User user = new User();
        user.setId(1L);
        user.setUsername("testuser");
        user.setPasswordHash("hash");
        user.setRole(role);
        return new UserPrincipal(user);
    }

    private CreateSeasonRequest buildCreateRequest() {
        CreateSeasonRequest r = new CreateSeasonRequest();
        r.setName("2024/2025");
        r.setDateFrom(LocalDate.of(2024, 9, 1));
        r.setDateTo(LocalDate.of(2025, 6, 30));
        return r;
    }

    private SeasonResponse buildSeasonResponse() {
        SeasonResponse r = new SeasonResponse();
        r.setId(1L);
        r.setName("2024/2025");
        r.setDateFrom(LocalDate.of(2024, 9, 1));
        r.setDateTo(LocalDate.of(2025, 6, 30));
        r.setStatus(Season.SeasonStatus.OPEN);
        return r;
    }
}
