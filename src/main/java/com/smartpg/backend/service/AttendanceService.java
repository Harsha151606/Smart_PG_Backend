package com.smartpg.backend.service;

import com.smartpg.backend.dto.AttendanceRequest;
import com.smartpg.backend.entity.Attendance;
import com.smartpg.backend.entity.Student;
import com.smartpg.backend.repository.AttendanceRepository;
import com.smartpg.backend.repository.StudentRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.time.LocalDate;

@Service
public class AttendanceService {

    private final AttendanceRepository attendanceRepository;
    private final StudentRepository studentRepository;

    public AttendanceService(AttendanceRepository attendanceRepository, StudentRepository studentRepository) {
        this.attendanceRepository = attendanceRepository;
        this.studentRepository = studentRepository;
    }

    public Attendance markAttendance(AttendanceRequest request) {
        Student student = studentRepository.findById(request.getStudentId())
                .orElseThrow(() -> new RuntimeException("Student not found"));

        List<Attendance> existing = attendanceRepository.findByDate(request.getDate());
        for (Attendance a : existing) {
            if (a.getStudent().getId().equals(request.getStudentId())) {
                a.setStatus(request.getStatus());
                return attendanceRepository.save(a);
            }
        }

        Attendance attendance = new Attendance(student, request.getDate(), request.getStatus());
        return attendanceRepository.save(attendance);
    }

    public List<Attendance> getStudentAttendance(Long studentId) {
        return attendanceRepository.findByStudentId(studentId);
    }

    public List<Attendance> getAttendanceByDate(LocalDate date) {
        return attendanceRepository.findByDate(date);
    }
}
