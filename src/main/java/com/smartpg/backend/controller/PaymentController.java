package com.smartpg.backend.controller;

import com.smartpg.backend.entity.Payment;
import com.smartpg.backend.security.StudentSecurityService;
import com.smartpg.backend.service.PaymentService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/payments")
@CrossOrigin
public class PaymentController {

    private final PaymentService paymentService;
    private final StudentSecurityService studentSecurityService;

    public PaymentController(PaymentService paymentService, StudentSecurityService studentSecurityService) {
        this.paymentService = paymentService;
        this.studentSecurityService = studentSecurityService;
    }

    // Create payment - OWNER & ADMIN
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN')")
    @PostMapping
    public ResponseEntity<Payment> createPayment(
            @RequestParam Long studentId,
            @RequestParam BigDecimal amount) {

        Payment payment = paymentService.createPayment(
                studentId,
                amount
        );

        return ResponseEntity.ok(payment);
    }

    // Get all payments - OWNER & ADMIN
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN')")
    @GetMapping
    public ResponseEntity<List<Payment>> getAllPayments() {

        return ResponseEntity.ok(
                paymentService.getAllPayments()
        );
    }

    // Get payments by student - STUDENT (own only) or OWNER / ADMIN
    @PreAuthorize("hasAnyRole('STUDENT', 'OWNER', 'ADMIN')")
    @GetMapping("/student/{studentId}")
    public ResponseEntity<List<Payment>> getPaymentsByStudent(
            @PathVariable Long studentId,
            Authentication authentication) {

        studentSecurityService.validateStudentAccess(studentId, authentication);

        return ResponseEntity.ok(
                paymentService.getPaymentsByStudent(studentId)
        );
    }

    // Get payments by status - OWNER & ADMIN
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN')")
    @GetMapping("/status/{status}")
    public ResponseEntity<List<Payment>> getPaymentsByStatus(
            @PathVariable String status) {

        return ResponseEntity.ok(
                paymentService.getPaymentsByStatus(status)
        );
    }
}