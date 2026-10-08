package com.smartpg.backend.security;

import com.smartpg.backend.entity.Role;
import com.smartpg.backend.entity.Student;
import com.smartpg.backend.entity.User;
import com.smartpg.backend.repository.StudentRepository;
import com.smartpg.backend.repository.UserRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

@Service
public class StudentSecurityService {

    private final UserRepository userRepository;
    private final StudentRepository studentRepository;

    public StudentSecurityService(UserRepository userRepository, StudentRepository studentRepository) {
        this.userRepository = userRepository;
        this.studentRepository = studentRepository;
    }

    public void validateStudentAccess(Long studentId, Authentication authentication) {
        if (authentication == null) {
            throw new AccessDeniedException("Unauthorized");
        }

        String email = authentication.getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new AccessDeniedException("User not found"));

        // ADMIN, OWNER, and WARDEN can access student records in their domain
        if (user.getRole() == Role.ADMIN || user.getRole() == Role.OWNER || user.getRole() == Role.WARDEN) {
            return;
        }

        // STUDENT must only access their own record
        if (user.getRole() == Role.STUDENT) {
            Student student = studentRepository.findByUserId(user.getId())
                    .orElseThrow(() -> new AccessDeniedException("Student profile not found for user"));

            if (!student.getId().equals(studentId)) {
                throw new AccessDeniedException("Access denied: You cannot access or modify another student's data");
            }
        }
    }
}
