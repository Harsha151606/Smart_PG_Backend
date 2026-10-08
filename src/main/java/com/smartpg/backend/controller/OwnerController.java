package com.smartpg.backend.controller;

import com.smartpg.backend.dto.PGRequest;
import com.smartpg.backend.dto.RoomRequest;
import com.smartpg.backend.dto.StudentRequest;
import com.smartpg.backend.entity.PG;
import com.smartpg.backend.entity.Room;
import com.smartpg.backend.entity.Student;
import com.smartpg.backend.service.PGService;
import com.smartpg.backend.service.RoomService;
import com.smartpg.backend.service.StudentService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/owner")
@PreAuthorize("hasAnyRole('OWNER', 'ADMIN')")
@CrossOrigin
public class OwnerController {

    private final PGService pgService;
    private final RoomService roomService;
    private final StudentService studentService;

    public OwnerController(PGService pgService,
                           RoomService roomService,
                           StudentService studentService) {
        this.pgService = pgService;
        this.roomService = roomService;
        this.studentService = studentService;
    }

    // --- PG Endpoints ---
    @PostMapping("/pgs")
    public ResponseEntity<PG> createPG(
            @Valid @RequestBody PGRequest request,
            Authentication authentication) {
        String ownerEmail = authentication.getName();
        PG pg = pgService.createPG(request, ownerEmail);
        return ResponseEntity.status(HttpStatus.CREATED).body(pg);
    }

    @GetMapping("/pgs")
    public ResponseEntity<List<PG>> getMyPGs(Authentication authentication) {
        String ownerEmail = authentication.getName();
        return ResponseEntity.ok(pgService.getPGsByOwner(ownerEmail));
    }

    // --- Room Endpoints ---
    @PostMapping("/rooms")
    public ResponseEntity<Room> createRoom(
            @Valid @RequestBody RoomRequest request,
            Authentication authentication) {
        String ownerEmail = authentication.getName();
        Room room = roomService.createRoom(request, ownerEmail);
        return ResponseEntity.status(HttpStatus.CREATED).body(room);
    }

    @GetMapping("/rooms")
    public ResponseEntity<List<Room>> getAllRooms(Authentication authentication) {
        String ownerEmail = authentication.getName();
        return ResponseEntity.ok(roomService.getAllRoomsForOwner(ownerEmail));
    }

    @GetMapping("/pgs/{pgId}/rooms")
    public ResponseEntity<List<Room>> getRooms(
            @PathVariable Long pgId,
            Authentication authentication) {
        String ownerEmail = authentication.getName();
        return ResponseEntity.ok(roomService.getRoomsByPG(pgId, ownerEmail));
    }

    @PutMapping("/rooms/{id}")
    public ResponseEntity<Room> updateRoom(
            @PathVariable Long id,
            @Valid @RequestBody RoomRequest request,
            Authentication authentication) {
        String ownerEmail = authentication.getName();
        return ResponseEntity.ok(roomService.updateRoom(id, request, ownerEmail));
    }

    @DeleteMapping("/rooms/{id}")
    public ResponseEntity<Map<String, String>> deleteRoom(
            @PathVariable Long id,
            Authentication authentication) {
        String ownerEmail = authentication.getName();
        roomService.deleteRoom(id, ownerEmail);
        return ResponseEntity.ok(Map.of("message", "Room deleted successfully"));
    }

    // --- Student Endpoints ---
    @PostMapping("/pgs/{pgId}/students")
    public ResponseEntity<Student> createStudent(
            @PathVariable Long pgId,
            @Valid @RequestBody StudentRequest request,
            Authentication authentication) {
        String ownerEmail = authentication.getName();
        Student student = studentService.createStudent(request, ownerEmail, pgId);
        return ResponseEntity.status(HttpStatus.CREATED).body(student);
    }

    @GetMapping("/students")
    public ResponseEntity<List<Student>> getAllStudents(Authentication authentication) {
        String ownerEmail = authentication.getName();
        return ResponseEntity.ok(studentService.getAllStudentsForOwner(ownerEmail));
    }

    @GetMapping("/pgs/{pgId}/students")
    public ResponseEntity<List<Student>> getStudentsByPG(
            @PathVariable Long pgId,
            Authentication authentication) {
        String ownerEmail = authentication.getName();
        return ResponseEntity.ok(studentService.getStudentsByPG(pgId, ownerEmail));
    }

    @PutMapping("/students/{id}")
    public ResponseEntity<Student> updateStudent(
            @PathVariable Long id,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String email,
            Authentication authentication) {
        String ownerEmail = authentication.getName();
        return ResponseEntity.ok(studentService.updateStudent(id, name, email, ownerEmail));
    }

    @DeleteMapping("/students/{id}")
    public ResponseEntity<Map<String, String>> deleteStudent(
            @PathVariable Long id,
            Authentication authentication) {
        String ownerEmail = authentication.getName();
        studentService.deleteStudent(id, ownerEmail);
        return ResponseEntity.ok(Map.of("message", "Student deleted successfully"));
    }

    // --- Stats Endpoint ---
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getOwnerStats(Authentication authentication) {
        String ownerEmail = authentication.getName();
        List<PG> pgs = pgService.getPGsByOwner(ownerEmail);
        List<Room> rooms = roomService.getAllRoomsForOwner(ownerEmail);
        List<Student> students = studentService.getAllStudentsForOwner(ownerEmail);

        long totalCapacity = rooms.stream().mapToLong(r -> r.getCapacity() != null ? r.getCapacity() : 0).sum();
        long totalOccupied = rooms.stream().mapToLong(r -> r.getOccupied() != null ? r.getOccupied() : 0).sum();

        Map<String, Object> stats = new HashMap<>();
        stats.put("totalPGs", pgs.size());
        stats.put("totalRooms", rooms.size());
        stats.put("totalStudents", students.size());
        stats.put("totalCapacity", totalCapacity);
        stats.put("totalOccupied", totalOccupied);
        stats.put("availableCapacity", Math.max(0, totalCapacity - totalOccupied));

        return ResponseEntity.ok(stats);
    }
}