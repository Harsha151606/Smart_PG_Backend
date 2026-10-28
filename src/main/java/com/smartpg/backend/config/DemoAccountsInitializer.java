package com.smartpg.backend.config;

import com.smartpg.backend.entity.Role;
import com.smartpg.backend.entity.User;
import com.smartpg.backend.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Creates predictable demo accounts only when explicitly enabled.
 * These shared credentials are intended for a demonstration environment,
 * not for real tenant or financial data.
 */
@Component
@Order(2)
public class DemoAccountsInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DemoAccountsInitializer.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.demo-users.enabled:false}")
    private boolean enabled;

    public DemoAccountsInitializer(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (!enabled) {
            log.info("Demo account provisioning is disabled.");
            return;
        }

        seed("Demo Student", "student@gmail.com", "student123", Role.STUDENT);
        seed("Demo Owner", "owner@gmail.com", "owner123", Role.OWNER);
        seed("Demo Warden", "warden@gmail.com", "warden123", Role.WARDEN);
    }

    private void seed(String name, String email, String rawPassword, Role role) {
        String normalizedEmail = email.trim().toLowerCase();
        User user = userRepository.findByEmail(normalizedEmail).orElse(null);

        if (user != null && user.getRole() == Role.ADMIN) {
            log.error("Cannot provision demo {} account because {} is already an ADMIN account.", role, normalizedEmail);
            return;
        }

        if (user == null) {
            user = new User();
            user.setEmail(normalizedEmail);
        }

        // These reserved demo identities are reset to known credentials at startup.
        user.setName(name);
        user.setPassword(passwordEncoder.encode(rawPassword));
        user.setRole(role);
        userRepository.save(user);
        log.info("Demo {} account provisioned for {}", role, normalizedEmail);
    }
}
