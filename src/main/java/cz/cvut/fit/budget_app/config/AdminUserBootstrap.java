package cz.cvut.fit.budget_app.config;

import cz.cvut.fit.budget_app.entity.User;
import cz.cvut.fit.budget_app.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Creates an ADMIN account when missing (controlled first-run setup only).
 * Keep disabled in production via {@code app.bootstrap.admin.enabled=false}.
 */
@Component
@ConditionalOnProperty(name = "app.bootstrap.admin.enabled", havingValue = "true")
@RequiredArgsConstructor
@Slf4j
public class AdminUserBootstrap implements ApplicationRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.bootstrap.admin.username:admin}")
    private String adminUsername;

    @Value("${app.bootstrap.admin.password:}")
    private String adminPassword;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        validateBootstrapCredentials();
        if (userRepository.findByUsername(adminUsername).isPresent()) {
            return;
        }
        User admin = new User();
        admin.setUsername(adminUsername);
        admin.setPasswordHash(passwordEncoder.encode(adminPassword));
        admin.setRole(User.Role.ADMIN);
        userRepository.save(admin);
        log.warn("Bootstrap: created admin user '{}'. Disable bootstrap after first setup.", adminUsername);
    }

    private void validateBootstrapCredentials() {
        if (adminPassword == null || adminPassword.isBlank()) {
            throw new IllegalStateException(
                    "Admin bootstrap is enabled but BOOTSTRAP_ADMIN_PASSWORD is empty. "
                            + "Provide a strong password or disable BOOTSTRAP_ADMIN."
            );
        }
        if ("admin123".equals(adminPassword)) {
            throw new IllegalStateException(
                    "Admin bootstrap is enabled with insecure default password 'admin123'. "
                            + "Set BOOTSTRAP_ADMIN_PASSWORD to a strong value or disable BOOTSTRAP_ADMIN."
            );
        }
    }
}
