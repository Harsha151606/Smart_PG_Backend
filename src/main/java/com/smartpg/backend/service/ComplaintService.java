package com.smartpg.backend.service;

import com.smartpg.backend.dto.ComplaintRequest;
import com.smartpg.backend.entity.Complaint;
import com.smartpg.backend.entity.Student;
import com.smartpg.backend.repository.ComplaintRepository;
import com.smartpg.backend.repository.StudentRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ComplaintService {

    private final ComplaintRepository complaintRepository;
    private final StudentRepository studentRepository;

    public ComplaintService(ComplaintRepository complaintRepository, StudentRepository studentRepository) {
        this.complaintRepository = complaintRepository;
        this.studentRepository = studentRepository;
    }

    public Complaint submitComplaint(ComplaintRequest request) {
        Student student = studentRepository.findById(request.getStudentId())
                .orElseThrow(() -> new RuntimeException("Student not found"));

        Complaint complaint = new Complaint(
                student, request.getTitle(), request.getDescription(), Complaint.ComplaintStatus.OPEN, java.time.LocalDateTime.now()
        );
        return complaintRepository.save(complaint);
    }

    public List<Complaint> getStudentComplaints(Long studentId) {
        return complaintRepository.findByStudentId(studentId);
    }

    public List<Complaint> getAllComplaints() {
        return complaintRepository.findAll();
    }

    public Complaint updateComplaintStatus(Long id, Complaint.ComplaintStatus status) {
        Complaint complaint = complaintRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Complaint not found"));
        complaint.setStatus(status);
        return complaintRepository.save(complaint);
    }
}
