package com.smartpg.backend.controller;

import com.smartpg.backend.dto.LoginRequest;
import com.smartpg.backend.dto.LoginResponse;
import com.smartpg.backend.dto.RegisterRequest;
import com.smartpg.backend.entity.User;
import com.smartpg.backend.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "http://localhost:5173")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public ResponseEntity<User> register(
            @Valid @RequestBody RegisterRequest request) {

        User user = authService.register(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(user);
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(
            @Valid @RequestBody LoginRequest request) {

        LoginResponse response =
                authService.login(request);

        return ResponseEntity.ok(response);
    }
}