package com.smartpg.backend.service;

import com.smartpg.backend.dto.LoginRequest;
import com.smartpg.backend.dto.LoginResponse;
import com.smartpg.backend.dto.RegisterRequest;
import com.smartpg.backend.entity.Role;
import com.smartpg.backend.entity.User;
import com.smartpg.backend.repository.UserRepository;
import com.smartpg.backend.security.JwtService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Set;

@Service
public class AuthService {

    private static final Set<Role> PUBLIC_REGISTRATION_ROLES = Set.of(
            Role.STUDENT,
            Role.OWNER,
            Role.WARDEN
    );

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService) {

        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    public User register(RegisterRequest request) {
        if (request.getRole() == null || !PUBLIC_REGISTRATION_ROLES.contains(request.getRole())) {
            throw new IllegalArgumentException(
                    "Invalid registration role. Public registration is restricted to STUDENT, OWNER, and WARDEN."
            );
        }

        String email = request.getEmail() != null ? request.getEmail().trim().toLowerCase() : "";
        if (userRepository.existsByEmail(email)) {
            throw new RuntimeException("Email already registered");
        }

        User user = new User();

        user.setName(request.getName() != null ? request.getName().trim() : "");
        user.setEmail(email);

        user.setPassword(
                passwordEncoder.encode(request.getPassword() != null ? request.getPassword() : "")
        );

        user.setRole(request.getRole());

        return userRepository.save(user);
    }

    public LoginResponse login(LoginRequest request) {
        String email = request.getEmail() != null ? request.getEmail().trim().toLowerCase() : "";
        User user = userRepository
                .findByEmail(email)
                .orElseThrow(() ->
                        new RuntimeException("Invalid email or password")
                );

        String rawPassword = request.getPassword() != null ? request.getPassword() : "";
        boolean passwordMatches = passwordEncoder.matches(rawPassword, user.getPassword());

        if (!passwordMatches) {
            throw new RuntimeException("Invalid email or password");
        }

        String token = jwtService.generateToken(
                user.getEmail(),
                user.getRole().name()
        );

        return new LoginResponse(
                token,
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getRole().name()
        );
    }
}