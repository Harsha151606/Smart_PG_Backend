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

import java.util.List;
import java.util.Optional;

@Component
@Order(1)
public class AdminInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminInitializer.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.admin.email:}")
    private String adminEmail;

    @Value("${app.admin.password:}")
    private String adminPassword;

    @Value("${app.admin.name:System Administrator}")
    private String adminName;

    @Value("${app.admin.sync-existing:false}")
    private boolean syncExistingAdmin;

    public AdminInitializer(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(String... args) {
        long adminCount = userRepository.countByRole(Role.ADMIN);

        if (adminCount > 1) {
            log.error("Found {} ADMIN accounts. No account was deleted or changed; resolve duplicate admin accounts manually.", adminCount);
            return;
        }

        String normalizedEmail = adminEmail == null ? "" : adminEmail.trim().toLowerCase();
        if (normalizedEmail.isBlank() || adminPassword == null || adminPassword.isBlank()) {
            log.error("Admin provisioning skipped: app.admin.email and app.admin.password must be configured.");
            return;
        }

        if (adminCount == 1) {
            User existingAdmin = userRepository.findAll().stream()
                    .filter(user -> user.getRole() == Role.ADMIN)
                    .findFirst()
                    .orElseThrow();

            if (!syncExistingAdmin) {
                log.info("System Administrator account already exists. Preserving existing admin credentials.");
                return;
            }

            Optional<User> emailOwner = userRepository.findByEmail(normalizedEmail);
            if (emailOwner.isPresent() && !emailOwner.get().getId().equals(existingAdmin.getId())) {
                log.error("Admin credentials were not synchronized because {} belongs to a different user.", normalizedEmail);
                return;
            }

            existingAdmin.setName(adminName == null || adminName.isBlank() ? "System Administrator" : adminName.trim());
            existingAdmin.setEmail(normalizedEmail);
            existingAdmin.setPassword(passwordEncoder.encode(adminPassword));
            userRepository.save(existingAdmin);
            log.info("Existing single ADMIN account synchronized to configured credentials for {}.", normalizedEmail);
            return;
        }

        // Never promote an existing non-admin account just because it has the configured admin email.
        if (userRepository.existsByEmail(normalizedEmail)) {
            log.error("Admin account was not created because {} already belongs to a non-admin user.", normalizedEmail);
            return;
        }

        User admin = new User();
        admin.setName(adminName == null || adminName.isBlank() ? "System Administrator" : adminName.trim());
        admin.setEmail(normalizedEmail);
        admin.setPassword(passwordEncoder.encode(adminPassword));
        admin.setRole(Role.ADMIN);
        userRepository.save(admin);

        log.info("Initial System Administrator account successfully provisioned for {}.", normalizedEmail);
    }
}
