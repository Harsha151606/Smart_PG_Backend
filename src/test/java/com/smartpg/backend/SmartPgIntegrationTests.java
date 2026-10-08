package com.smartpg.backend;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartpg.backend.dto.*;
import com.smartpg.backend.entity.*;
import com.smartpg.backend.repository.*;
import com.smartpg.backend.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
public class SmartPgIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PGRepository pgRepository;

    @Autowired
    private RoomRepository roomRepository;

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private JwtService jwtService;

    private String adminToken;
    private String ownerToken;
    private String wardenToken;
    private String studentAToken;
    @BeforeEach
    void setUp() {
        // Generate JWT tokens directly
        adminToken = jwtService.generateToken("admin_test@example.com", "ADMIN");
        ownerToken = jwtService.generateToken("owner_test@example.com", "OWNER");
        wardenToken = jwtService.generateToken("warden_test@example.com", "WARDEN");
        studentAToken = jwtService.generateToken("student_a_test@example.com", "STUDENT");
        jwtService.generateToken("student_b_test@example.com", "STUDENT");
    }

    // ========================================================
    // PHASE 4 — AUTHENTICATION TESTING
    // ========================================================
    @Test
    @DisplayName("Phase 4: Registration and Login workflows")
    void testAuthWorkflow() throws Exception {
        String uniqueSuffix = String.valueOf(System.currentTimeMillis());
        RegisterRequest regReq = new RegisterRequest();
        regReq.setName("New Student");
        regReq.setEmail("student_" + uniqueSuffix + "@test.com");
        regReq.setPassword("Password@123");
        regReq.setRole(Role.STUDENT);

        // 1. Valid registration
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(regReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email", is(regReq.getEmail())))
                .andExpect(jsonPath("$.password").doesNotExist()); // Password never exposed!

        // 2. Duplicate registration
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(regReq)))
                .andExpect(status().isConflict());

        // 3. Invalid registration (empty fields)
        RegisterRequest invalidReq = new RegisterRequest();
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidReq)))
                .andExpect(status().isBadRequest());

        // 4. Valid login (must return JWT without needing pre-existing token)
        LoginRequest loginReq = new LoginRequest();
        loginReq.setEmail(regReq.getEmail());
        loginReq.setPassword("Password@123");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token", notNullValue()))
                .andExpect(jsonPath("$.role", is("STUDENT")))
                .andExpect(jsonPath("$.password").doesNotExist());

        // 5. Wrong password
        loginReq.setPassword("WrongPass!");
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginReq)))
                .andExpect(status().isUnauthorized());

        // 6. Nonexistent user
        loginReq.setEmail("nonexistent_user_999@test.com");
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginReq)))
                .andExpect(status().isUnauthorized());

        // 7. Missing token -> 401
        mockMvc.perform(get("/api/admin/users"))
                .andExpect(status().isUnauthorized());

        // 8. Invalid token -> 401
        mockMvc.perform(get("/api/admin/users")
                        .header("Authorization", "Bearer invalid.jwt.token"))
                .andExpect(status().isUnauthorized());
    }

    // ========================================================
    // PHASE 5 — ROLE AUTHORIZATION TESTING
    // ========================================================
    @Test
    @DisplayName("Phase 5: Role authorization - allowed vs forbidden")
    void testRoleAuthorization() throws Exception {
        // Admin endpoint: Admin -> allowed (200)
        mockMvc.perform(get("/api/admin/stats")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());

        // Admin endpoint: Owner -> forbidden (403)
        mockMvc.perform(get("/api/admin/stats")
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isForbidden());

        // Admin endpoint: Student -> forbidden (403)
        mockMvc.perform(get("/api/admin/stats")
                        .header("Authorization", "Bearer " + studentAToken))
                .andExpect(status().isForbidden());

        // Owner endpoint: Student -> forbidden (403)
        mockMvc.perform(get("/api/owner/pgs")
                        .header("Authorization", "Bearer " + studentAToken))
                .andExpect(status().isForbidden());

        // Warden endpoint: Student -> forbidden (403)
        mockMvc.perform(get("/api/warden/stats")
                        .header("Authorization", "Bearer " + studentAToken))
                .andExpect(status().isForbidden());
    }

    // ========================================================
    // PHASE 8 — ROOM ASSIGNMENT & CAPACITY RULES
    // ========================================================
    @Test
    @DisplayName("Phase 8: Room capacity assignment and removal consistency")
    void testRoomAssignmentCapacityRules() throws Exception {
        String suffix = String.valueOf(System.currentTimeMillis());

        // Create owner user
        User owner = new User("Owner " + suffix, "owner_" + suffix + "@pg.com", "pass", Role.OWNER);
        owner = userRepository.save(owner);
        String currentOwnerToken = jwtService.generateToken(owner.getEmail(), "OWNER");

        // Create PG
        PG pg = new PG("Test PG " + suffix, "123 Main St", owner);
        pg = pgRepository.save(pg);

        // Create Room with capacity = 3
        Room room = new Room("R-" + suffix, 3, 5000.0, pg);
        room.setOccupied(0);
        room = roomRepository.save(room);

        // Create 4 students
        User u1 = userRepository.save(new User("S1 " + suffix, "s1_" + suffix + "@pg.com", "pass", Role.STUDENT));
        User u2 = userRepository.save(new User("S2 " + suffix, "s2_" + suffix + "@pg.com", "pass", Role.STUDENT));
        User u3 = userRepository.save(new User("S3 " + suffix, "s3_" + suffix + "@pg.com", "pass", Role.STUDENT));
        User u4 = userRepository.save(new User("S4 " + suffix, "s4_" + suffix + "@pg.com", "pass", Role.STUDENT));

        Student s1 = studentRepository.save(new Student(u1, pg, null));
        Student s2 = studentRepository.save(new Student(u2, pg, null));
        Student s3 = studentRepository.save(new Student(u3, pg, null));
        Student s4 = studentRepository.save(new Student(u4, pg, null));

        // 1. Assign student 1 -> occupied = 1
        mockMvc.perform(post("/api/room-assignment/assign")
                        .param("studentId", s1.getId().toString())
                        .param("roomId", room.getId().toString())
                        .header("Authorization", "Bearer " + currentOwnerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.room.occupied", is(1)));

        // 2. Assign same student again -> rejected (400)
        mockMvc.perform(post("/api/room-assignment/assign")
                        .param("studentId", s1.getId().toString())
                        .param("roomId", room.getId().toString())
                        .header("Authorization", "Bearer " + currentOwnerToken))
                .andExpect(status().isBadRequest());

        // 3. Assign student 2 -> occupied = 2
        mockMvc.perform(post("/api/room-assignment/assign")
                        .param("studentId", s2.getId().toString())
                        .param("roomId", room.getId().toString())
                        .header("Authorization", "Bearer " + currentOwnerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.room.occupied", is(2)));

        // 4. Assign student 3 -> occupied = 3 (full)
        mockMvc.perform(post("/api/room-assignment/assign")
                        .param("studentId", s3.getId().toString())
                        .param("roomId", room.getId().toString())
                        .header("Authorization", "Bearer " + currentOwnerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.room.occupied", is(3)));

        // 5. Assign student 4 -> MUST BE REJECTED (capacity full)
        mockMvc.perform(post("/api/room-assignment/assign")
                        .param("studentId", s4.getId().toString())
                        .param("roomId", room.getId().toString())
                        .header("Authorization", "Bearer " + currentOwnerToken))
                .andExpect(status().isBadRequest());

        // 6. Remove student 2 -> occupied = 2
        mockMvc.perform(delete("/api/room-assignment/remove/" + s2.getId())
                        .header("Authorization", "Bearer " + currentOwnerToken))
                .andExpect(status().isOk());

        // Verify occupied count in DB is 2
        Room updatedRoom = roomRepository.findById(room.getId()).orElseThrow();
        org.junit.jupiter.api.Assertions.assertEquals(2, updatedRoom.getOccupied());

        // 7. Remove student who has no room -> rejected (400)
        mockMvc.perform(delete("/api/room-assignment/remove/" + s2.getId())
                        .header("Authorization", "Bearer " + currentOwnerToken))
                .andExpect(status().isBadRequest());
    }

    // ========================================================
    // PHASE 9 — PAYMENT MODULE TESTING
    // ========================================================
    @Test
    @DisplayName("Phase 9: Payment CRUD, validations, and role restrictions")
    void testPaymentModule() throws Exception {
        String suffix = String.valueOf(System.currentTimeMillis());
        User owner = userRepository.save(new User("PO " + suffix, "po_" + suffix + "@pg.com", "pass", Role.OWNER));
        String poToken = jwtService.generateToken(owner.getEmail(), "OWNER");
        User stuUser = userRepository.save(new User("PS " + suffix, "ps_" + suffix + "@pg.com", "pass", Role.STUDENT));
        String psToken = jwtService.generateToken(stuUser.getEmail(), "STUDENT");
        Student student = studentRepository.save(new Student(stuUser, null, null));

        // 1. Valid payment by owner
        mockMvc.perform(post("/api/payments")
                        .param("studentId", student.getId().toString())
                        .param("amount", "6000.00")
                        .header("Authorization", "Bearer " + poToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("PAID")))
                .andExpect(jsonPath("$.amount", is(6000.00)));

        // 2. Invalid student ID -> 404 / 400
        mockMvc.perform(post("/api/payments")
                        .param("studentId", "999999")
                        .param("amount", "1000.00")
                        .header("Authorization", "Bearer " + poToken))
                .andExpect(status().isNotFound());

        // 3. Invalid amount (<= 0) -> 400
        mockMvc.perform(post("/api/payments")
                        .param("studentId", student.getId().toString())
                        .param("amount", "-50.00")
                        .header("Authorization", "Bearer " + poToken))
                .andExpect(status().isBadRequest());

        // 4. Student cannot access all payments -> 403
        mockMvc.perform(get("/api/payments")
                        .header("Authorization", "Bearer " + psToken))
                .andExpect(status().isForbidden());

        // 5. Warden cannot access payments -> 403
        mockMvc.perform(get("/api/payments")
                        .header("Authorization", "Bearer " + wardenToken))
                .andExpect(status().isForbidden());

        // 6. Student can access their own payments
        mockMvc.perform(get("/api/payments/student/" + student.getId())
                        .header("Authorization", "Bearer " + psToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(1))));
    }

    // ========================================================
    // PHASE 12 & 13 — LEAVE & COMPLAINTS WORKFLOW
    // ========================================================
    @Test
    @DisplayName("Phase 12 & 13: Leave request & Complaint lifecycles")
    void testLeaveAndComplaintWorkflow() throws Exception {
        String suffix = String.valueOf(System.currentTimeMillis());
        User stuUser = userRepository.save(new User("LS " + suffix, "ls_" + suffix + "@pg.com", "pass", Role.STUDENT));
        String stuToken = jwtService.generateToken(stuUser.getEmail(), "STUDENT");
        Student student = studentRepository.save(new Student(stuUser, null, null));

        // 1. Submit valid leave request
        String validLeaveJson = String.format("{\"studentId\":%d,\"startDate\":\"%s\",\"endDate\":\"%s\",\"reason\":\"Semester break\"}",
                student.getId(), LocalDate.now().plusDays(1), LocalDate.now().plusDays(5));

        mockMvc.perform(post("/api/leaves")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validLeaveJson)
                        .header("Authorization", "Bearer " + stuToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("PENDING")));

        // 2. Submit invalid leave dates (endDate < startDate) -> rejected (400)
        String invalidLeaveJson = String.format("{\"studentId\":%d,\"startDate\":\"%s\",\"endDate\":\"%s\",\"reason\":\"Invalid dates\"}",
                student.getId(), LocalDate.now().plusDays(5), LocalDate.now().plusDays(1));

        mockMvc.perform(post("/api/leaves")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidLeaveJson)
                        .header("Authorization", "Bearer " + stuToken))
                .andExpect(status().isBadRequest());

        // 3. Submit valid complaint
        ComplaintRequest complaintReq = new ComplaintRequest();
        complaintReq.setStudentId(student.getId());
        complaintReq.setTitle("WiFi issue");
        complaintReq.setDescription("WiFi not working on 2nd floor");

        mockMvc.perform(post("/api/complaints")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(complaintReq))
                        .header("Authorization", "Bearer " + stuToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("OPEN")));
    }

    // ========================================================
    // PHASE 14 — STUDENT DATA SECURITY (IDOR TESTING)
    // ========================================================
    @Test
    @DisplayName("Phase 14: Student data security - Student A cannot access Student B's data")
    void testStudentDataSecurityIdor() throws Exception {
        String suffix = String.valueOf(System.currentTimeMillis());

        // Student A
        User userA = userRepository.save(new User("Alice " + suffix, "alice_" + suffix + "@pg.com", "pass", Role.STUDENT));
        String tokenA = jwtService.generateToken(userA.getEmail(), "STUDENT");
        Student studentA = studentRepository.save(new Student(userA, null, null));

        // Student B
        User userB = userRepository.save(new User("Bob " + suffix, "bob_" + suffix + "@pg.com", "pass", Role.STUDENT));
        String tokenB = jwtService.generateToken(userB.getEmail(), "STUDENT");
        Student studentB = studentRepository.save(new Student(userB, null, null));

        // Test 1: Student B tries to access Student A's payments -> 403
        mockMvc.perform(get("/api/payments/student/" + studentA.getId())
                        .header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isForbidden());

        // Test 2: Student B tries to access Student A's complaints -> 403
        mockMvc.perform(get("/api/complaints/student/" + studentA.getId())
                        .header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isForbidden());

        // Test 3: Student B tries to submit a complaint impersonating Student A -> 403
        ComplaintRequest compReq = new ComplaintRequest();
        compReq.setStudentId(studentA.getId());
        compReq.setTitle("Fake Complaint");
        compReq.setDescription("Impersonation test");

        mockMvc.perform(post("/api/complaints")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(compReq))
                        .header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isForbidden());

        // Test 4: Student B tries to access Student A's leave requests -> 403
        mockMvc.perform(get("/api/leaves/student/" + studentA.getId())
                        .header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isForbidden());

        // Test 5: Student B tries to submit leave request for Student A -> 403
        String fakeLeaveJson = String.format("{\"studentId\":%d,\"startDate\":\"%s\",\"endDate\":\"%s\",\"reason\":\"Impersonation leave\"}",
                studentA.getId(), LocalDate.now().plusDays(2), LocalDate.now().plusDays(4));

        mockMvc.perform(post("/api/leaves")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(fakeLeaveJson)
                        .header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isForbidden());

        // Test 6: Student B tries to access Student A's attendance -> 403
        mockMvc.perform(get("/api/attendance/student/" + studentA.getId())
                        .header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isForbidden());
    }

    // ========================================================
    // PHASE 6 — ADMIN MODULE CRUD
    // ========================================================
    @Test
    @DisplayName("Phase 6: Admin user CRUD and system statistics")
    void testAdminModuleCrud() throws Exception {
        String suffix = String.valueOf(System.currentTimeMillis());

        // 1. List users
        mockMvc.perform(get("/api/admin/users")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(1))));

        // 2. Create user
        UserManageRequest createReq = new UserManageRequest("Temp Warden " + suffix, "tempw_" + suffix + "@pg.com", "Password@123", Role.WARDEN);
        String res = mockMvc.perform(post("/api/admin/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createReq))
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", notNullValue()))
                .andReturn().getResponse().getContentAsString();

        Long createdId = objectMapper.readTree(res).get("id").asLong();

        // 3. Update user
        createReq.setName("Updated Warden Name");
        mockMvc.perform(put("/api/admin/users/" + createdId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createReq))
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name", is("Updated Warden Name")));

        // 4. Delete user
        mockMvc.perform(delete("/api/admin/users/" + createdId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());

        // 5. System stats
        mockMvc.perform(get("/api/admin/stats")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalUsers", notNullValue()))
                .andExpect(jsonPath("$.totalPGs", notNullValue()));
    }

    // ========================================================
    // PHASE 10 & 11 — WARDEN & ATTENDANCE MODULE
    // ========================================================
    @Test
    @DisplayName("Phase 10 & 11: Warden students/rooms viewing and attendance recording")
    void testWardenAndAttendance() throws Exception {
        String suffix = String.valueOf(System.currentTimeMillis());
        User stuUser = userRepository.save(new User("Att Student " + suffix, "att_" + suffix + "@pg.com", "pass", Role.STUDENT));
        Student student = studentRepository.save(new Student(stuUser, null, null));

        // 1. Warden lists students & rooms
        mockMvc.perform(get("/api/warden/students")
                        .header("Authorization", "Bearer " + wardenToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(1))));

        mockMvc.perform(get("/api/warden/rooms")
                        .header("Authorization", "Bearer " + wardenToken))
                .andExpect(status().isOk());

        // 2. Warden gets stats
        mockMvc.perform(get("/api/warden/stats")
                        .header("Authorization", "Bearer " + wardenToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalStudents", notNullValue()));

        // 3. Mark PRESENT
        String attJson = String.format("{\"studentId\":%d,\"date\":\"%s\",\"status\":\"PRESENT\"}",
                student.getId(), LocalDate.now().toString());

        mockMvc.perform(post("/api/attendance")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(attJson)
                        .header("Authorization", "Bearer " + wardenToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("PRESENT")));

        // 4. Update to ABSENT
        String attAbsentJson = String.format("{\"studentId\":%d,\"date\":\"%s\",\"status\":\"ABSENT\"}",
                student.getId(), LocalDate.now().toString());

        mockMvc.perform(post("/api/attendance")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(attAbsentJson)
                        .header("Authorization", "Bearer " + wardenToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("ABSENT")));

        // 5. View by date
        mockMvc.perform(get("/api/attendance/date/" + LocalDate.now().toString())
                        .header("Authorization", "Bearer " + wardenToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(1))));

        // 6. Student unauthorized to mark attendance -> 403
        mockMvc.perform(post("/api/attendance")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(attJson)
                        .header("Authorization", "Bearer " + studentAToken))
                .andExpect(status().isForbidden());
    }
}
