package com.smartpg.backend.dto;

import com.smartpg.backend.entity.Attendance;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public class AttendanceRequest {
    @NotNull
    private Long studentId;
    @NotNull
    private LocalDate date;
    @NotNull
    private Attendance.AttendanceStatus status;

    public Long getStudentId() { return studentId; }
    public void setStudentId(Long studentId) { this.studentId = studentId; }
    public LocalDate getDate() { return date; }
    public void setDate(LocalDate date) { this.date = date; }
    public Attendance.AttendanceStatus getStatus() { return status; }
    public void setStatus(Attendance.AttendanceStatus status) { this.status = status; }
}
