package com.smartpg.backend.controller;

import com.smartpg.backend.dto.UserManageRequest;
import com.smartpg.backend.entity.PG;
import com.smartpg.backend.entity.User;
import com.smartpg.backend.service.AdminService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
@CrossOrigin
public class AdminController {

    private final AdminService adminService;

    public AdminController(AdminService adminService) {
        this.adminService = adminService;
    }

    // User management
    @GetMapping("/users")
    public ResponseEntity<List<User>> getAllUsers() {
        return ResponseEntity.ok(adminService.getAllUsers());
    }

    @PostMapping("/users")
    public ResponseEntity<User> createUser(@Valid @RequestBody UserManageRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(adminService.createUser(request));
    }

    @PutMapping("/users/{id}")
    public ResponseEntity<User> updateUser(
            @PathVariable Long id,
            @Valid @RequestBody UserManageRequest request) {
        return ResponseEntity.ok(adminService.updateUser(id, request));
    }

    @DeleteMapping("/users/{id}")
    public ResponseEntity<Map<String, String>> deleteUser(@PathVariable Long id) {
        adminService.deleteUser(id);
        return ResponseEntity.ok(Map.of("message", "User deleted successfully"));
    }

    // PG management
    @GetMapping("/pgs")
    public ResponseEntity<List<PG>> getAllPGs() {
        return ResponseEntity.ok(adminService.getAllPGs());
    }

    @PostMapping("/pgs")
    public ResponseEntity<PG> createPG(
            @RequestParam String name,
            @RequestParam String address,
            @RequestParam Long ownerId) {
        return ResponseEntity.status(HttpStatus.CREATED).body(adminService.createPG(name, address, ownerId));
    }

    @DeleteMapping("/pgs/{id}")
    public ResponseEntity<Map<String, String>> deletePG(@PathVariable Long id) {
        adminService.deletePG(id);
        return ResponseEntity.ok(Map.of("message", "PG deleted successfully"));
    }

    // Statistics
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getSystemStats() {
        return ResponseEntity.ok(adminService.getSystemStats());
    }
}
