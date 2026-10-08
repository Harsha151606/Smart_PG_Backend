package com.smartpg.backend.controller;

import com.smartpg.backend.dto.PGRequest;
import com.smartpg.backend.dto.RoomRequest;
import com.smartpg.backend.entity.PG;
import com.smartpg.backend.entity.Room;
import com.smartpg.backend.service.PGService;
import com.smartpg.backend.service.RoomService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import com.smartpg.backend.dto.StudentRequest;
import com.smartpg.backend.entity.Student;
import com.smartpg.backend.service.StudentService;

@RestController
@RequestMapping("/api/owner")
@CrossOrigin(origins = "http://localhost:5173")
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

    @PostMapping("/pgs")
    public ResponseEntity<PG> createPG(
            @Valid @RequestBody PGRequest request,
            Authentication authentication) {

        String ownerEmail = authentication.getName();

        PG pg = pgService.createPG(request, ownerEmail);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(pg);
    }
    @GetMapping("/pgs/{pgId}/rooms")
    public ResponseEntity<java.util.List<Room>> getRooms(
            @PathVariable Long pgId,
            Authentication authentication) {
            
        String ownerEmail = authentication.getName();
            
        java.util.List<Room> rooms =
                roomService.getRoomsByPG(pgId, ownerEmail);
            
        return ResponseEntity.ok(rooms);
    }
    @PostMapping("/pgs/{pgId}/students")
    public ResponseEntity<Student> createStudent(
            @PathVariable Long pgId,
            @Valid @RequestBody StudentRequest request,
            Authentication authentication) {

        String ownerEmail = authentication.getName();

        Student student = studentService.createStudent(
                request,
                ownerEmail,
                pgId
        );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(student);
    }
    @PostMapping("/rooms")
    public ResponseEntity<Room> createRoom(
            @Valid @RequestBody RoomRequest request,
            Authentication authentication) {

        String ownerEmail = authentication.getName();

        Room room = roomService.createRoom(request, ownerEmail);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(room);
    }
}