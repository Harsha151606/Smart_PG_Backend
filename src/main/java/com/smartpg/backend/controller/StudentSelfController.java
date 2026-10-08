package com.smartpg.backend.controller;

import com.smartpg.backend.entity.User;
import com.smartpg.backend.repository.StudentRepository;
import com.smartpg.backend.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

@RestController
@RequestMapping("/api/student")
public class StudentSelfController {

    private final StudentRepository studentRepository;
    private final UserRepository userRepository;

    public StudentSelfController(StudentRepository studentRepository, UserRepository userRepository) {
        this.studentRepository = studentRepository;
        this.userRepository = userRepository;
    }

    /**
     * Returns the Student profile for the currently authenticated user.
     * If the user is logged in but not yet registered as a student (e.g. self-registered with STUDENT role
     * but the owner hasn't created a Student record yet), returns a clear 404 message.
     */
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/me")
    public ResponseEntity<?> getMyStudentProfile(Authentication authentication) {
        String email = authentication.getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        return studentRepository.findByUserId(user.getId())
                .<ResponseEntity<?>>map(ResponseEntity::ok)
                .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", "No student profile found. Ask your owner to register you as a student.")));
    }
}
