package com.smartpg.backend.service;

import com.smartpg.backend.dto.UserManageRequest;
import com.smartpg.backend.entity.*;
import com.smartpg.backend.repository.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class AdminService {

    private final UserRepository userRepository;
    private final PGRepository pgRepository;
    private final RoomRepository roomRepository;
    private final StudentRepository studentRepository;
    private final PaymentRepository paymentRepository;
    private final ComplaintRepository complaintRepository;
    private final LeaveRequestRepository leaveRequestRepository;
    private final PasswordEncoder passwordEncoder;

    public AdminService(
            UserRepository userRepository,
            PGRepository pgRepository,
            RoomRepository roomRepository,
            StudentRepository studentRepository,
            PaymentRepository paymentRepository,
            ComplaintRepository complaintRepository,
            LeaveRequestRepository leaveRequestRepository,
            PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.pgRepository = pgRepository;
        this.roomRepository = roomRepository;
        this.studentRepository = studentRepository;
        this.paymentRepository = paymentRepository;
        this.complaintRepository = complaintRepository;
        this.leaveRequestRepository = leaveRequestRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    public User createUser(UserManageRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email already registered");
        }
        if (request.getPassword() == null || request.getPassword().isBlank()) {
            throw new IllegalArgumentException("Password is required for new user");
        }

        User user = new User();
        user.setName(request.getName());
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRole(request.getRole());

        return userRepository.save(user);
    }

    public User updateUser(Long id, UserManageRequest request) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (!user.getEmail().equalsIgnoreCase(request.getEmail()) &&
                userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email already in use");
        }

        user.setName(request.getName());
        user.setEmail(request.getEmail());
        user.setRole(request.getRole());

        if (request.getPassword() != null && !request.getPassword().isBlank()) {
            user.setPassword(passwordEncoder.encode(request.getPassword()));
        }

        return userRepository.save(user);
    }

    @Transactional
    public void deleteUser(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // If user is a student, remove student record first
        studentRepository.findByUserId(id).ifPresent(student -> {
            if (student.getRoom() != null) {
                Room room = student.getRoom();
                if (room.getOccupied() > 0) {
                    room.setOccupied(room.getOccupied() - 1);
                    roomRepository.save(room);
                }
            }
            studentRepository.delete(student);
        });

        // If user is owner, delete their PGs or disassociate
        List<PG> pgs = pgRepository.findByOwner(user);
        for (PG pg : pgs) {
            List<Room> rooms = roomRepository.findByPg(pg);
            roomRepository.deleteAll(rooms);
            pgRepository.delete(pg);
        }

        userRepository.delete(user);
    }

    public List<PG> getAllPGs() {
        return pgRepository.findAll();
    }

    public PG createPG(String name, String address, Long ownerId) {
        User owner = userRepository.findById(ownerId)
                .orElseThrow(() -> new RuntimeException("Owner not found"));

        if (owner.getRole() != Role.OWNER && owner.getRole() != Role.ADMIN) {
            throw new RuntimeException("Assigned user must be an OWNER or ADMIN");
        }

        PG pg = new PG(name, address, owner);
        return pgRepository.save(pg);
    }

    @Transactional
    public void deletePG(Long id) {
        PG pg = pgRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("PG not found"));

        List<Room> rooms = roomRepository.findByPg(pg);
        List<Student> students = studentRepository.findByPg(pg);

        for (Student s : students) {
            s.setRoom(null);
            s.setPg(null);
            studentRepository.save(s);
        }

        roomRepository.deleteAll(rooms);
        pgRepository.delete(pg);
    }

    public Map<String, Object> getSystemStats() {
        Map<String, Object> stats = new HashMap<>();

        long totalUsers = userRepository.count();
        long totalOwners = userRepository.countByRole(Role.OWNER);
        long totalWardens = userRepository.countByRole(Role.WARDEN);
        long totalStudents = userRepository.countByRole(Role.STUDENT);
        long totalAdmins = userRepository.countByRole(Role.ADMIN);

        long totalPGs = pgRepository.count();
        List<Room> rooms = roomRepository.findAll();
        long totalRooms = rooms.size();
        long totalCapacity = rooms.stream().mapToLong(r -> r.getCapacity() != null ? r.getCapacity() : 0).sum();
        long totalOccupied = rooms.stream().mapToLong(r -> r.getOccupied() != null ? r.getOccupied() : 0).sum();

        List<Payment> payments = paymentRepository.findAll();
        long totalPaymentsCount = payments.size();
        BigDecimal totalRevenue = payments.stream()
                .filter(p -> "PAID".equalsIgnoreCase(p.getStatus()))
                .map(Payment::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        long totalComplaints = complaintRepository.count();
        long openComplaints = complaintRepository.findByStatus(Complaint.ComplaintStatus.OPEN).size();

        long totalLeaves = leaveRequestRepository.count();
        long pendingLeaves = leaveRequestRepository.findByStatus(LeaveRequest.LeaveStatus.PENDING).size();

        stats.put("totalUsers", totalUsers);
        stats.put("totalOwners", totalOwners);
        stats.put("totalWardens", totalWardens);
        stats.put("totalStudents", totalStudents);
        stats.put("totalAdmins", totalAdmins);

        stats.put("totalPGs", totalPGs);
        stats.put("totalRooms", totalRooms);
        stats.put("totalCapacity", totalCapacity);
        stats.put("totalOccupied", totalOccupied);
        stats.put("availableCapacity", Math.max(0, totalCapacity - totalOccupied));

        stats.put("totalPaymentsCount", totalPaymentsCount);
        stats.put("totalRevenue", totalRevenue);

        stats.put("totalComplaints", totalComplaints);
        stats.put("openComplaints", openComplaints);

        stats.put("totalLeaves", totalLeaves);
        stats.put("pendingLeaves", pendingLeaves);

        return stats;
    }
}
