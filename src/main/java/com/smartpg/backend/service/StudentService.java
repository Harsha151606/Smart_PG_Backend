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
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

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

        if (owner.getRole() != Role.OWNER && owner.getRole() != Role.ADMIN) {
            throw new RuntimeException("Only owners can create students");
        }

        PG pg;
        if (owner.getRole() == Role.ADMIN) {
            pg = pgRepository.findById(pgId)
                    .orElseThrow(() -> new RuntimeException("PG not found"));
        } else {
            pg = pgRepository.findByIdAndOwner(pgId, owner)
                    .orElseThrow(() -> new RuntimeException("PG not found or does not belong to owner"));
        }

        User studentUser = new User();
        studentUser.setName(request.getName());
        studentUser.setEmail(request.getEmail());
        studentUser.setPassword(passwordEncoder.encode(request.getPassword()));
        studentUser.setRole(Role.STUDENT);

        User savedUser = userRepository.save(studentUser);

        Student student = new Student(savedUser, pg, null);
        return studentRepository.save(student);
    }

    public List<Student> getAllStudentsForOwner(String ownerEmail) {
        User owner = userRepository.findByEmail(ownerEmail)
                .orElseThrow(() -> new RuntimeException("Owner not found"));

        if (owner.getRole() == Role.ADMIN) {
            return studentRepository.findAll();
        }

        List<PG> pgs = pgRepository.findByOwner(owner);
        List<Student> allStudents = new ArrayList<>();
        for (PG pg : pgs) {
            allStudents.addAll(studentRepository.findByPg(pg));
        }
        return allStudents;
    }

    public List<Student> getStudentsByPG(Long pgId, String ownerEmail) {
        User owner = userRepository.findByEmail(ownerEmail)
                .orElseThrow(() -> new RuntimeException("Owner not found"));

        PG pg;
        if (owner.getRole() == Role.ADMIN) {
            pg = pgRepository.findById(pgId)
                    .orElseThrow(() -> new RuntimeException("PG not found"));
        } else {
            pg = pgRepository.findByIdAndOwner(pgId, owner)
                    .orElseThrow(() -> new RuntimeException("PG not found or does not belong to owner"));
        }

        return studentRepository.findByPg(pg);
    }

    public Student updateStudent(Long studentId, String name, String email, String ownerEmail) {
        User owner = userRepository.findByEmail(ownerEmail)
                .orElseThrow(() -> new RuntimeException("Owner not found"));

        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new RuntimeException("Student not found"));

        if (owner.getRole() != Role.ADMIN && (student.getPg() == null || !student.getPg().getOwner().getId().equals(owner.getId()))) {
            throw new RuntimeException("You do not have permission to update this student");
        }

        User user = student.getUser();
        if (name != null && !name.isBlank()) {
            user.setName(name);
        }
        if (email != null && !email.isBlank()) {
            if (!email.equalsIgnoreCase(user.getEmail()) && userRepository.existsByEmail(email)) {
                throw new RuntimeException("Email already in use");
            }
            user.setEmail(email);
        }
        userRepository.save(user);

        return studentRepository.save(student);
    }

    @Transactional
    public void deleteStudent(Long studentId, String ownerEmail) {
        User owner = userRepository.findByEmail(ownerEmail)
                .orElseThrow(() -> new RuntimeException("Owner not found"));

        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new RuntimeException("Student not found"));

        if (owner.getRole() != Role.ADMIN && (student.getPg() == null || !student.getPg().getOwner().getId().equals(owner.getId()))) {
            throw new RuntimeException("You do not have permission to delete this student");
        }

        if (student.getRoom() != null) {
            Room room = student.getRoom();
            if (room.getOccupied() > 0) {
                room.setOccupied(room.getOccupied() - 1);
                roomRepository.save(room);
            }
            student.setRoom(null);
        }

        User user = student.getUser();
        studentRepository.delete(student);
        if (user != null) {
            userRepository.delete(user);
        }
    }
}