package cz.cvut.fit.budget_app.controller;

import cz.cvut.fit.budget_app.entity.User;
import cz.cvut.fit.budget_app.entity.Transaction;
import cz.cvut.fit.budget_app.security.JwtAuthenticationFilter;
import cz.cvut.fit.budget_app.security.JwtTokenProvider;
import cz.cvut.fit.budget_app.security.UserDetailsServiceImpl;
import cz.cvut.fit.budget_app.security.UserPrincipal;
import cz.cvut.fit.budget_app.service.ExportService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ExportController.class)
@Import({JwtAuthenticationFilter.class})
class ExportControllerTest {

    @Autowired MockMvc mockMvc;

    @MockitoBean ExportService exportService;
    @MockitoBean JwtTokenProvider jwtTokenProvider;
    @MockitoBean UserDetailsServiceImpl userDetailsService;

    @Test
    void exportTransactions_asTreasurer_returns200WithCsvHeaders() throws Exception {
        when(exportService.exportTransactionsCsv(1L, null, false)).thenReturn("id,date\n");

        mockMvc.perform(get("/api/export/transactions/1")
                        .with(user(principalFor(User.Role.TREASURER))))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", "attachment; filename=\"transactions-season-1.csv\""))
                .andExpect(header().string("Content-Type", "text/csv; charset=UTF-8"));
    }

    @Test
    void exportTransactions_withStatusAndIncludeProposed_passesFiltersToService() throws Exception {
        when(exportService.exportTransactionsCsv(1L, Transaction.ApprovalStatus.APPROVED, true))
                .thenReturn("id,date\n");

        mockMvc.perform(get("/api/export/transactions/1")
                        .param("status", "APPROVED")
                        .param("includeProposed", "true")
                        .with(user(principalFor(User.Role.TREASURER))))
                .andExpect(status().isOk());

        verify(exportService).exportTransactionsCsv(1L, Transaction.ApprovalStatus.APPROVED, true);
    }

    @Test
    void exportBudgetItems_asMember_returns403() throws Exception {
        mockMvc.perform(get("/api/export/budget-items/1")
                        .with(user(principalFor(User.Role.MEMBER))))
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
}
