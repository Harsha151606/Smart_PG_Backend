package com.smartpg.backend.controller;

import com.smartpg.backend.dto.ComplaintRequest;
import com.smartpg.backend.entity.Complaint;
import com.smartpg.backend.security.StudentSecurityService;
import com.smartpg.backend.service.ComplaintService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/complaints")
public class ComplaintController {

    private final ComplaintService complaintService;
    private final StudentSecurityService studentSecurityService;

    public ComplaintController(ComplaintService complaintService, StudentSecurityService studentSecurityService) {
        this.complaintService = complaintService;
        this.studentSecurityService = studentSecurityService;
    }

    @PreAuthorize("hasRole('STUDENT')")
    @PostMapping
    public ResponseEntity<Complaint> submitComplaint(
            @Valid @RequestBody ComplaintRequest request,
            Authentication authentication) {
        studentSecurityService.validateStudentAccess(request.getStudentId(), authentication);
        return ResponseEntity.ok(complaintService.submitComplaint(request));
    }

    @PreAuthorize("hasAnyRole('STUDENT', 'WARDEN', 'OWNER', 'ADMIN')")
    @GetMapping("/student/{studentId}")
    public ResponseEntity<List<Complaint>> getStudentComplaints(
            @PathVariable Long studentId,
            Authentication authentication) {
        studentSecurityService.validateStudentAccess(studentId, authentication);
        return ResponseEntity.ok(complaintService.getStudentComplaints(studentId));
    }

    @PreAuthorize("hasAnyRole('WARDEN', 'OWNER', 'ADMIN')")
    @GetMapping
    public ResponseEntity<List<Complaint>> getAllComplaints() {
        return ResponseEntity.ok(complaintService.getAllComplaints());
    }

    @PreAuthorize("hasAnyRole('WARDEN', 'OWNER', 'ADMIN')")
    @PutMapping("/{id}/status")
    public ResponseEntity<Complaint> updateComplaintStatus(
            @PathVariable Long id,
            @RequestParam Complaint.ComplaintStatus status) {
        return ResponseEntity.ok(complaintService.updateComplaintStatus(id, status));
    }
}
