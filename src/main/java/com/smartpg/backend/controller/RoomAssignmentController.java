package com.smartpg.backend.controller;

import com.smartpg.backend.entity.Student;
import com.smartpg.backend.service.RoomAssignmentService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/room-assignment")
@CrossOrigin
public class RoomAssignmentController {

    private final RoomAssignmentService roomAssignmentService;

    public RoomAssignmentController(RoomAssignmentService roomAssignmentService) {
        this.roomAssignmentService = roomAssignmentService;
    }

    @PostMapping("/assign")
    public ResponseEntity<Student> assignStudentToRoom(
            @RequestParam Long studentId,
            @RequestParam Long roomId) {

        Student student = roomAssignmentService
                .assignStudentToRoom(studentId, roomId);

        return ResponseEntity.ok(student);
    }

    @DeleteMapping("/remove/{studentId}")
    public ResponseEntity<Student> removeStudentFromRoom(
            @PathVariable Long studentId) {

        Student student = roomAssignmentService
                .removeStudentFromRoom(studentId);

        return ResponseEntity.ok(student);
    }
}