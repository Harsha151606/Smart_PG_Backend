package com.smartpg.backend.service;

import com.smartpg.backend.entity.Room;
import com.smartpg.backend.entity.Student;
import com.smartpg.backend.repository.RoomRepository;
import com.smartpg.backend.repository.StudentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RoomAssignmentService {

    private final RoomRepository roomRepository;
    private final StudentRepository studentRepository;

    public RoomAssignmentService(RoomRepository roomRepository,
                                 StudentRepository studentRepository) {
        this.roomRepository = roomRepository;
        this.studentRepository = studentRepository;
    }

    @Transactional
    public Student assignStudentToRoom(Long studentId, Long roomId) {

        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new RuntimeException("Student not found"));

        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new RuntimeException("Room not found"));

        if (student.getRoom() != null) {
            throw new RuntimeException("Student is already assigned to a room");
        }

        if (room.getOccupied() >= room.getCapacity()) {
            throw new RuntimeException("Room is already full");
        }

        student.setRoom(room);
        room.setOccupied(room.getOccupied() + 1);
        roomRepository.save(room);

        return studentRepository.save(student);
    }

    @Transactional
    public Student removeStudentFromRoom(Long studentId) {

        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new RuntimeException("Student not found"));

        if (student.getRoom() == null) {
            throw new RuntimeException("Student is not assigned to any room");
        }

        Room room = student.getRoom();
        student.setRoom(null);

        if (room.getOccupied() > 0) {
            room.setOccupied(room.getOccupied() - 1);
        }

        roomRepository.save(room);

        return studentRepository.save(student);
    }
}