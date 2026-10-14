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

@Component
@Order(1)
public class AdminInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminInitializer.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.admin.email:admin@smartpg.com}")
    private String adminEmail;

    @Value("${app.admin.password:password123}")
    private String adminPassword;

    @Value("${app.admin.name:System Administrator}")
    private String adminName;

    public AdminInitializer(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(String... args) {
        long adminCount = userRepository.countByRole(Role.ADMIN);
        if (adminCount > 0) {
            log.info("System Administrator account already exists (count: {}). Preserving existing admin.", adminCount);
            return;
        }

        String normalizedEmail = adminEmail != null ? adminEmail.trim().toLowerCase() : "admin@smartpg.com";
        if (userRepository.existsByEmail(normalizedEmail)) {
            User existingUser = userRepository.findByEmail(normalizedEmail).orElseThrow();
            existingUser.setRole(Role.ADMIN);
            userRepository.save(existingUser);
            log.info("Assigned ADMIN role to existing user account: {}", normalizedEmail);
            return;
        }

        User admin = new User();
        admin.setName(adminName != null ? adminName.trim() : "System Administrator");
        admin.setEmail(normalizedEmail);
        admin.setPassword(passwordEncoder.encode(adminPassword != null ? adminPassword : "password123"));
        admin.setRole(Role.ADMIN);

        userRepository.save(admin);
        log.info("Initial System Administrator account successfully provisioned for: {}", normalizedEmail);
    }
}
