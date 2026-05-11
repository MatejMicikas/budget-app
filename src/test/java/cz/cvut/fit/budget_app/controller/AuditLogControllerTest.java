package cz.cvut.fit.budget_app.controller;

import cz.cvut.fit.budget_app.dto.response.AuditLogResponse;
import cz.cvut.fit.budget_app.entity.AuditLog;
import cz.cvut.fit.budget_app.entity.User;
import cz.cvut.fit.budget_app.security.JwtAuthenticationFilter;
import cz.cvut.fit.budget_app.security.JwtTokenProvider;
import cz.cvut.fit.budget_app.security.UserDetailsServiceImpl;
import cz.cvut.fit.budget_app.security.UserPrincipal;
import cz.cvut.fit.budget_app.service.AuditLogService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuditLogController.class)
@Import({JwtAuthenticationFilter.class})
class AuditLogControllerTest {

    @Autowired MockMvc mockMvc;

    @MockitoBean AuditLogService auditLogService;
    @MockitoBean JwtTokenProvider jwtTokenProvider;
    @MockitoBean UserDetailsServiceImpl userDetailsService;

    @Test
    void findAll_asAdmin_returns200() throws Exception {
        when(auditLogService.findAll()).thenReturn(List.of(new AuditLogResponse()));

        mockMvc.perform(get("/api/audit-logs")
                        .with(user(principalFor(User.Role.ADMIN))))
                .andExpect(status().isOk());
    }

    @Test
    void findByOperationType_asAdmin_returns200() throws Exception {
        when(auditLogService.findByOperationType(AuditLog.OperationType.USER_ROLE_CHANGED))
                .thenReturn(List.of(new AuditLogResponse()));

        mockMvc.perform(get("/api/audit-logs")
                        .param("operationType", "USER_ROLE_CHANGED")
                        .with(user(principalFor(User.Role.ADMIN))))
                .andExpect(status().isOk());
    }

    @Test
    void findByOperationType_invalidValue_returns400() throws Exception {
        mockMvc.perform(get("/api/audit-logs")
                        .param("operationType", "CRE")
                        .with(user(principalFor(User.Role.ADMIN))))
                .andExpect(status().isBadRequest());
    }

    private UserPrincipal principalFor(User.Role role) {
        User user = new User();
        user.setId(1L);
        user.setUsername("testuser");
        user.setPasswordHash("hash");
        user.setRole(role);
        return new UserPrincipal(user);
    }
}
