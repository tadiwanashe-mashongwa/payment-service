package com.example.paymentservice.controller;

import com.example.paymentservice.dto.CreatePaymentRequest;
import com.example.paymentservice.dto.PaymentResponse;
import com.example.paymentservice.dto.UpdatePaymentStatusRequest;
import com.example.paymentservice.entity.Payment;
import com.example.paymentservice.service.PaymentService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping
    public ResponseEntity<PaymentResponse> createPayment(@Valid @RequestBody CreatePaymentRequest request) {
        Payment payment = paymentService.initiatePayment(
                request.orderId(),
                request.customerId(),
                request.amount()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(PaymentResponse.from(payment));
    }

    @GetMapping("/{paymentId}")
    public PaymentResponse getPayment(@PathVariable UUID paymentId) {
        return PaymentResponse.from(paymentService.getPayment(paymentId));
    }

    @GetMapping("/customer/{customerId}")
    public Page<PaymentResponse> getPaymentsByCustomer(
            @PathVariable UUID customerId,
            Pageable pageable
    ) {
        return paymentService.getPaymentsByCustomer(customerId, pageable);
    }

    @PatchMapping("/{paymentId}/status")
    public PaymentResponse updatePaymentStatus(
            @PathVariable UUID paymentId,
            @Valid @RequestBody UpdatePaymentStatusRequest request
    ) {
        return PaymentResponse.from(paymentService.transitionPaymentStatus(paymentId, request.status()));
    }
}
