package com.smartpg.backend.controller;

import com.smartpg.backend.entity.*;
import com.smartpg.backend.repository.*;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/warden")
@PreAuthorize("hasAnyRole('WARDEN', 'ADMIN')")
@CrossOrigin
public class WardenController {

    private final StudentRepository studentRepository;
    private final RoomRepository roomRepository;
    private final AttendanceRepository attendanceRepository;
    private final LeaveRequestRepository leaveRequestRepository;
    private final ComplaintRepository complaintRepository;

    public WardenController(StudentRepository studentRepository,
                            RoomRepository roomRepository,
                            AttendanceRepository attendanceRepository,
                            LeaveRequestRepository leaveRequestRepository,
                            ComplaintRepository complaintRepository) {
        this.studentRepository = studentRepository;
        this.roomRepository = roomRepository;
        this.attendanceRepository = attendanceRepository;
        this.leaveRequestRepository = leaveRequestRepository;
        this.complaintRepository = complaintRepository;
    }

    @GetMapping("/students")
    public ResponseEntity<List<Student>> getAllStudents() {
        return ResponseEntity.ok(studentRepository.findAll());
    }

    @GetMapping("/rooms")
    public ResponseEntity<List<Room>> getAllRooms() {
        return ResponseEntity.ok(roomRepository.findAll());
    }

    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getWardenStats() {
        Map<String, Object> stats = new HashMap<>();

        long totalStudents = studentRepository.count();
        long totalRooms = roomRepository.count();

        LocalDate today = LocalDate.now();
        List<Attendance> todayAttendance = attendanceRepository.findByDate(today);
        long presentToday = todayAttendance.stream()
                .filter(a -> a.getStatus() == Attendance.AttendanceStatus.PRESENT)
                .count();
        long absentToday = todayAttendance.stream()
                .filter(a -> a.getStatus() == Attendance.AttendanceStatus.ABSENT)
                .count();

        long pendingLeaves = leaveRequestRepository.findByStatus(LeaveRequest.LeaveStatus.PENDING).size();
        long openComplaints = complaintRepository.findByStatus(Complaint.ComplaintStatus.OPEN).size();

        stats.put("totalStudents", totalStudents);
        stats.put("totalRooms", totalRooms);
        stats.put("markedAttendanceToday", todayAttendance.size());
        stats.put("presentToday", presentToday);
        stats.put("absentToday", absentToday);
        stats.put("pendingLeaves", pendingLeaves);
        stats.put("openComplaints", openComplaints);

        return ResponseEntity.ok(stats);
    }
}
