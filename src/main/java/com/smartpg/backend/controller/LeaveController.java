package com.smartpg.backend.controller;

import com.smartpg.backend.dto.LeaveRequestDto;
import com.smartpg.backend.entity.LeaveRequest;
import com.smartpg.backend.security.StudentSecurityService;
import com.smartpg.backend.service.LeaveService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/leaves")
public class LeaveController {

    private final LeaveService leaveService;
    private final StudentSecurityService studentSecurityService;

    public LeaveController(LeaveService leaveService, StudentSecurityService studentSecurityService) {
        this.leaveService = leaveService;
        this.studentSecurityService = studentSecurityService;
    }

    @PreAuthorize("hasRole('STUDENT')")
    @PostMapping
    public ResponseEntity<LeaveRequest> submitLeaveRequest(
            @Valid @RequestBody LeaveRequestDto request,
            Authentication authentication) {
        studentSecurityService.validateStudentAccess(request.getStudentId(), authentication);
        return ResponseEntity.ok(leaveService.submitLeaveRequest(request));
    }

    @PreAuthorize("hasAnyRole('STUDENT', 'WARDEN', 'OWNER', 'ADMIN')")
    @GetMapping("/student/{studentId}")
    public ResponseEntity<List<LeaveRequest>> getStudentLeaveRequests(
            @PathVariable Long studentId,
            Authentication authentication) {
        studentSecurityService.validateStudentAccess(studentId, authentication);
        return ResponseEntity.ok(leaveService.getStudentLeaveRequests(studentId));
    }

    @PreAuthorize("hasAnyRole('WARDEN', 'OWNER', 'ADMIN')")
    @GetMapping
    public ResponseEntity<List<LeaveRequest>> getAllLeaveRequests() {
        return ResponseEntity.ok(leaveService.getAllLeaveRequests());
    }

    @PreAuthorize("hasAnyRole('WARDEN', 'OWNER', 'ADMIN')")
    @PutMapping("/{id}/status")
    public ResponseEntity<LeaveRequest> updateLeaveStatus(
            @PathVariable Long id,
            @RequestParam LeaveRequest.LeaveStatus status) {
        return ResponseEntity.ok(leaveService.updateLeaveStatus(id, status));
    }
}
