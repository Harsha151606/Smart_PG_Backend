package com.smartpg.backend.service;

import com.smartpg.backend.entity.Payment;
import com.smartpg.backend.entity.Student;
import com.smartpg.backend.repository.PaymentRepository;
import com.smartpg.backend.repository.StudentRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Service
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final StudentRepository studentRepository;

    public PaymentService(
            PaymentRepository paymentRepository,
            StudentRepository studentRepository) {

        this.paymentRepository = paymentRepository;
        this.studentRepository = studentRepository;
    }

    // Create a payment
    public Payment createPayment(
            Long studentId,
            BigDecimal amount) {

        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Payment amount must be greater than zero");
        }

        Student student = studentRepository.findById(studentId)
                .orElseThrow(() ->
                        new RuntimeException("Student not found"));

        Payment payment = new Payment();

        payment.setStudent(student);
        payment.setAmount(amount);
        payment.setDate(LocalDate.now());
        payment.setStatus("PAID");

        return paymentRepository.save(payment);
    }

    // Get all payments
    public List<Payment> getAllPayments() {
        return paymentRepository.findAll();
    }

    // Get payments of a particular student
    public List<Payment> getPaymentsByStudent(
            Long studentId) {

        return paymentRepository
                .findByStudentId(studentId);
    }

    // Get payments by status
    public List<Payment> getPaymentsByStatus(
            String status) {

        return paymentRepository
                .findByStatus(status.toUpperCase());
    }
}