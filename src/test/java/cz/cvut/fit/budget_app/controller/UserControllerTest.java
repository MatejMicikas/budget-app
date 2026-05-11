package cz.cvut.fit.budget_app.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import cz.cvut.fit.budget_app.dto.request.ChangeRoleRequest;
import cz.cvut.fit.budget_app.dto.request.CreateUserRequest;
import cz.cvut.fit.budget_app.dto.response.UserResponse;
import cz.cvut.fit.budget_app.entity.User;
import cz.cvut.fit.budget_app.security.JwtAuthenticationFilter;
import cz.cvut.fit.budget_app.security.JwtTokenProvider;
import cz.cvut.fit.budget_app.security.SecurityConfig;
import cz.cvut.fit.budget_app.security.UserDetailsServiceImpl;
import cz.cvut.fit.budget_app.security.UserPrincipal;
import cz.cvut.fit.budget_app.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UserController.class)
@TestPropertySource(properties = "app.cors.allowed-origins=http://localhost:5173")
@Import({JwtAuthenticationFilter.class, SecurityConfig.class})
class UserControllerTest {

    @Autowired MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @MockitoBean UserService userService;
    @MockitoBean JwtTokenProvider jwtTokenProvider;
    @MockitoBean UserDetailsServiceImpl userDetailsService;

    @Test
    void findAll_asAdmin_returns200() throws Exception {
        when(userService.findAll()).thenReturn(List.of(new UserResponse()));

        mockMvc.perform(get("/api/users")
                        .with(user(principalFor(User.Role.ADMIN))))
                .andExpect(status().isOk());
    }

    @Test
    void findAll_asTreasurer_returns403() throws Exception {
        mockMvc.perform(get("/api/users")
                        .with(user(principalFor(User.Role.TREASURER))))
                .andExpect(status().isForbidden());
    }

    @Test
    void create_asAdmin_returns201() throws Exception {
        CreateUserRequest req = new CreateUserRequest();
        req.setUsername("newuser");
        req.setPassword("password123");
        req.setRole(User.Role.MEMBER);

        when(userService.create(any(), eq(1L))).thenReturn(new UserResponse());

        mockMvc.perform(post("/api/users")
                        .with(user(principalFor(User.Role.ADMIN)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated());

        verify(userService).create(any(), eq(1L));
    }

    @Test
    void changeRole_asAdmin_passesPrincipalId() throws Exception {
        ChangeRoleRequest req = new ChangeRoleRequest();
        req.setRole(User.Role.TREASURER);
        when(userService.changeRole(eq(5L), any(), eq(1L))).thenReturn(new UserResponse());

        mockMvc.perform(put("/api/users/5/role")
                        .with(user(principalFor(User.Role.ADMIN)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk());

        verify(userService).changeRole(eq(5L), any(), eq(1L));
    }

    @Test
    void changeRole_whenServiceRejectsSelfDemotion_returns409() throws Exception {
        ChangeRoleRequest req = new ChangeRoleRequest();
        req.setRole(User.Role.TREASURER);
        when(userService.changeRole(eq(1L), any(), eq(1L)))
                .thenThrow(new IllegalStateException("ADMIN cannot change their own role"));

        mockMvc.perform(put("/api/users/1/role")
                        .with(user(principalFor(User.Role.ADMIN)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isConflict());
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
