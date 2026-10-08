package com.smartpg.backend.controller;

import com.smartpg.backend.dto.AttendanceRequest;
import com.smartpg.backend.entity.Attendance;
import com.smartpg.backend.security.StudentSecurityService;
import com.smartpg.backend.service.AttendanceService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/attendance")
public class AttendanceController {

    private final AttendanceService attendanceService;
    private final StudentSecurityService studentSecurityService;

    public AttendanceController(AttendanceService attendanceService, StudentSecurityService studentSecurityService) {
        this.attendanceService = attendanceService;
        this.studentSecurityService = studentSecurityService;
    }

    @PreAuthorize("hasAnyRole('WARDEN', 'ADMIN')")
    @PostMapping
    public ResponseEntity<Attendance> markAttendance(@Valid @RequestBody AttendanceRequest request) {
        return ResponseEntity.ok(attendanceService.markAttendance(request));
    }

    @PreAuthorize("hasAnyRole('STUDENT', 'WARDEN', 'OWNER', 'ADMIN')")
    @GetMapping("/student/{studentId}")
    public ResponseEntity<List<Attendance>> getStudentAttendance(
            @PathVariable Long studentId,
            Authentication authentication) {
        studentSecurityService.validateStudentAccess(studentId, authentication);
        return ResponseEntity.ok(attendanceService.getStudentAttendance(studentId));
    }

    @PreAuthorize("hasAnyRole('WARDEN', 'OWNER', 'ADMIN')")
    @GetMapping("/date/{date}")
    public ResponseEntity<List<Attendance>> getAttendanceByDate(@PathVariable String date) {
        return ResponseEntity.ok(attendanceService.getAttendanceByDate(LocalDate.parse(date)));
    }
}
