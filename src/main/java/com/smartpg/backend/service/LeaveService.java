package com.smartpg.backend.service;

import com.smartpg.backend.dto.LeaveRequestDto;
import com.smartpg.backend.entity.LeaveRequest;
import com.smartpg.backend.entity.Student;
import com.smartpg.backend.repository.LeaveRequestRepository;
import com.smartpg.backend.repository.StudentRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class LeaveService {

    private final LeaveRequestRepository leaveRequestRepository;
    private final StudentRepository studentRepository;

    public LeaveService(LeaveRequestRepository leaveRequestRepository, StudentRepository studentRepository) {
        this.leaveRequestRepository = leaveRequestRepository;
        this.studentRepository = studentRepository;
    }

    public LeaveRequest submitLeaveRequest(LeaveRequestDto request) {
        if (request.getStartDate() == null || request.getEndDate() == null) {
            throw new IllegalArgumentException("Start date and end date are required");
        }
        if (request.getEndDate().isBefore(request.getStartDate())) {
            throw new IllegalArgumentException("End date cannot be before start date");
        }

        Student student = studentRepository.findById(request.getStudentId())
                .orElseThrow(() -> new RuntimeException("Student not found"));

        LeaveRequest leaveRequest = new LeaveRequest(
                student, request.getStartDate(), request.getEndDate(), request.getReason(), LeaveRequest.LeaveStatus.PENDING
        );
        return leaveRequestRepository.save(leaveRequest);
    }

    public List<LeaveRequest> getStudentLeaveRequests(Long studentId) {
        return leaveRequestRepository.findByStudentId(studentId);
    }

    public List<LeaveRequest> getAllLeaveRequests() {
        return leaveRequestRepository.findAll();
    }

    public LeaveRequest updateLeaveStatus(Long id, LeaveRequest.LeaveStatus status) {
        LeaveRequest request = leaveRequestRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Leave request not found"));
        request.setStatus(status);
        return leaveRequestRepository.save(request);
    }
}
