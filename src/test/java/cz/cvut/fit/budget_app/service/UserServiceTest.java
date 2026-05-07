package cz.cvut.fit.budget_app.service;

import cz.cvut.fit.budget_app.dto.request.ChangeRoleRequest;
import cz.cvut.fit.budget_app.dto.request.CreateUserRequest;
import cz.cvut.fit.budget_app.dto.response.UserResponse;
import cz.cvut.fit.budget_app.entity.User;
import cz.cvut.fit.budget_app.exception.ResourceNotFoundException;
import cz.cvut.fit.budget_app.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock UserRepository userRepository;
    @Mock AuditLogService auditLogService;
    @Mock PasswordEncoder passwordEncoder;

    @InjectMocks UserService userService;

    @Test
    void create_newUsername_savesAndReturnsUser() {
        when(userRepository.findByUsername("jan")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("password123")).thenReturn("hashed");
        when(userRepository.save(any())).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            u.setId(1L);
            return u;
        });

        CreateUserRequest req = new CreateUserRequest();
        req.setUsername("jan");
        req.setPassword("password123");
        req.setRole(User.Role.TREASURER);

        UserResponse result = userService.create(req);

        assertThat(result.getUsername()).isEqualTo("jan");
        assertThat(result.getRole()).isEqualTo(User.Role.TREASURER);
        verify(passwordEncoder).encode("password123");
    }

    @Test
    void create_duplicateUsername_throwsIllegalArgument() {
        when(userRepository.findByUsername("jan")).thenReturn(Optional.of(new User()));

        CreateUserRequest req = new CreateUserRequest();
        req.setUsername("jan");
        req.setPassword("password123");
        req.setRole(User.Role.MEMBER);

        assertThatThrownBy(() -> userService.create(req))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("already taken");
    }

    @Test
    void changeRole_existingUser_updatesRoleAndLogsAudit() {
        User user = buildUser(1L, User.Role.MEMBER);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ChangeRoleRequest req = new ChangeRoleRequest();
        req.setRole(User.Role.TREASURER);

        UserResponse result = userService.changeRole(1L, req, 99L);

        assertThat(result.getRole()).isEqualTo(User.Role.TREASURER);
        verify(auditLogService).logUserRoleChanged(1L, 99L);
    }

    @Test
    void findById_notFound_throwsResourceNotFoundException() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.findById(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ---- helpers ----

    private User buildUser(Long id, User.Role role) {
        User u = new User();
        u.setId(id);
        u.setUsername("testuser");
        u.setPasswordHash("hashed");
        u.setRole(role);
        return u;
    }
}
