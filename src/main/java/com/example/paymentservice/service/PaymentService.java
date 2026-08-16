package com.example.paymentservice.service;

import com.example.paymentservice.entity.Payment;
import com.example.paymentservice.entity.PaymentStatus;
import com.example.paymentservice.dto.PaymentResponse;
import com.example.paymentservice.exception.PaymentNotFoundException;
import com.example.paymentservice.repository.PaymentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.UUID;

@Service
public class PaymentService {

    private final PaymentRepository paymentRepository;

    public PaymentService(PaymentRepository paymentRepository) {
        this.paymentRepository = paymentRepository;
    }

    public Payment initiatePayment(UUID orderId, UUID customerId, BigDecimal amount) {
        return paymentRepository.save(new Payment(orderId, customerId, amount));
    }

    @Transactional
    public Payment initiatePaymentForOrder(UUID orderId, UUID customerId, BigDecimal amount) {
        return paymentRepository.findByOrderId(orderId)
                .orElseGet(() -> paymentRepository.save(new Payment(orderId, customerId, amount)));
    }

    public Payment getPayment(UUID paymentId) {
        return paymentRepository.findById(paymentId)
                .orElseThrow(() -> new PaymentNotFoundException(paymentId));
    }

    public Page<PaymentResponse> getPaymentsByCustomer(UUID customerId, Pageable pageable) {
        return paymentRepository.findByCustomerId(customerId, pageable)
                .map(PaymentResponse::from);
    }

    @Transactional
    public Payment transitionPaymentStatus(UUID paymentId, PaymentStatus targetStatus) {
        Payment payment = getPayment(paymentId);
        payment.transitionTo(targetStatus);
        return paymentRepository.save(payment);
    }
}
