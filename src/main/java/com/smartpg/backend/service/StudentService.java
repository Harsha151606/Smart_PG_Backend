package com.smartpg.backend.service;

import com.smartpg.backend.dto.StudentRequest;
import com.smartpg.backend.entity.PG;
import com.smartpg.backend.entity.Role;
import com.smartpg.backend.entity.Student;
import com.smartpg.backend.entity.Room;
import com.smartpg.backend.entity.User;
import com.smartpg.backend.repository.PGRepository;
import com.smartpg.backend.repository.RoomRepository;
import com.smartpg.backend.repository.StudentRepository;
import com.smartpg.backend.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class StudentService {

    private final StudentRepository studentRepository;
    private final UserRepository userRepository;
    private final PGRepository pgRepository;
    private final RoomRepository roomRepository;
    private final PasswordEncoder passwordEncoder;

    public StudentService(StudentRepository studentRepository,
            UserRepository userRepository,
            PGRepository pgRepository,
            RoomRepository roomRepository,
            PasswordEncoder passwordEncoder) {
        this.studentRepository = studentRepository;
        this.userRepository = userRepository;
        this.pgRepository = pgRepository;
        this.roomRepository = roomRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public Student createStudent(StudentRequest request,
            String ownerEmail,
            Long pgId) {

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email already registered");
        }

        User owner = userRepository.findByEmail(ownerEmail)
                .orElseThrow(() -> new RuntimeException("Owner not found"));

        if (owner.getRole() != Role.OWNER) {
            throw new RuntimeException("Only owners can create students");
        }

        PG pg = pgRepository.findByIdAndOwner(pgId, owner)
                .orElseThrow(() -> new RuntimeException("PG not found or does not belong to owner"));

        User studentUser = new User();

        studentUser.setName(request.getName());
        studentUser.setEmail(request.getEmail());
        studentUser.setPassword(
                passwordEncoder.encode(request.getPassword()));
        studentUser.setRole(Role.STUDENT);

        User savedUser = userRepository.save(studentUser);

        Student student = new Student(
                savedUser,
                pg,
                null);
        return studentRepository.save(student);
    }

    public Student assignRoom(Long studentId,
            Long roomId,
            String ownerEmail) {
        User owner = userRepository.findByEmail(ownerEmail)
                .orElseThrow(() -> new RuntimeException("Owner not found"));

        if (owner.getRole() != Role.OWNER) {
            throw new RuntimeException("Only owners can assign rooms");
        }

        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new RuntimeException("Student not found"));

        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new RuntimeException("Room not found"));

        PG pg = room.getPg();

        if (!pg.getOwner().getId().equals(owner.getId())) {
            throw new RuntimeException("You cannot assign rooms from another owner's PG");
        }

        if (student.getPg() == null ||
                !student.getPg().getId().equals(pg.getId())) {
            throw new RuntimeException("Student does not belong to this PG");
        }

        long occupied = studentRepository.countByRoom(room);

        if (occupied >= room.getCapacity()) {
            throw new RuntimeException("Room is already full");
        }

        student.setRoom(room);

        return studentRepository.save(student);
    }
}