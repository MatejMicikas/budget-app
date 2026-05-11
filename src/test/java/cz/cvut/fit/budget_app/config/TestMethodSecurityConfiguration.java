package cz.cvut.fit.budget_app.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;

/**
 * Activates {@code @PreAuthorize} in {@code @WebMvcTest} slices (the main app enables this via
 * {@link cz.cvut.fit.budget_app.security.SecurityConfig}).
 */
@TestConfiguration
@EnableMethodSecurity
public class TestMethodSecurityConfiguration {}
