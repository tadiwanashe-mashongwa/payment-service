package com.example.paymentservice.service;

import com.example.paymentservice.entity.Payment;
import com.example.paymentservice.entity.PaymentStatus;
import com.example.paymentservice.dto.PaymentResponse;
import com.example.paymentservice.event.PaymentStatusChangedEvent;
import com.example.paymentservice.exception.PaymentNotFoundException;
import com.example.paymentservice.outbox.PaymentOutboxEvent;
import com.example.paymentservice.outbox.PaymentOutboxEventRepository;
import com.example.paymentservice.client.CustomerContactClient;
import com.example.paymentservice.provider.MobileMoneyChargeRequest;
import com.example.paymentservice.provider.MobileMoneyProvider;
import com.example.paymentservice.repository.PaymentRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.UUID;

@Service
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final PaymentOutboxEventRepository paymentOutboxEventRepository;
    private final ObjectMapper objectMapper;
    private final CustomerContactClient customerContactClient;
    private final MobileMoneyProvider mobileMoneyProvider;

    public PaymentService(
            PaymentRepository paymentRepository,
            PaymentOutboxEventRepository paymentOutboxEventRepository,
            ObjectMapper objectMapper,
            CustomerContactClient customerContactClient,
            MobileMoneyProvider mobileMoneyProvider
    ) {
        this.paymentRepository = paymentRepository;
        this.paymentOutboxEventRepository = paymentOutboxEventRepository;
        this.objectMapper = objectMapper;
        this.customerContactClient = customerContactClient;
        this.mobileMoneyProvider = mobileMoneyProvider;
    }

    public Payment initiatePayment(UUID orderId, UUID customerId, BigDecimal amount) {
        return initiatePaymentForOrder(orderId, customerId, amount);
    }

    @Transactional
    public Payment initiatePaymentForOrder(UUID orderId, UUID customerId, BigDecimal amount) {
        var existingPayment = paymentRepository.findByOrderId(orderId);
        if (existingPayment.isPresent()) {
            return existingPayment.get();
        }

        Payment payment = paymentRepository.save(new Payment(orderId, customerId, amount));
        String phoneNumber = customerContactClient.paymentPhoneNumber(customerId);
        String providerReference = mobileMoneyProvider.initiateCharge(
                new MobileMoneyChargeRequest(orderId, customerId, phoneNumber, amount)
        );
        payment.assignProviderReference(providerReference);
        return paymentRepository.save(payment);
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
        return transitionPaymentStatus(getPayment(paymentId), targetStatus);
    }

    @Transactional
    public Payment handleProviderCallback(String providerReference, PaymentStatus targetStatus) {
        Payment payment = paymentRepository.findByProviderReference(providerReference)
                .orElseThrow(() -> new IllegalArgumentException("Payment provider reference not found: " + providerReference));
        return transitionPaymentStatus(payment, targetStatus);
    }

    private Payment transitionPaymentStatus(Payment payment, PaymentStatus targetStatus) {
        if (payment.getStatus() == targetStatus) {
            return payment;
        }
        payment.transitionTo(targetStatus);
        Payment savedPayment = paymentRepository.save(payment);
        PaymentStatusChangedEvent event = new PaymentStatusChangedEvent(
                savedPayment.getId(), savedPayment.getOrderId(), savedPayment.getStatus()
        );
        paymentOutboxEventRepository.save(new PaymentOutboxEvent(
                savedPayment.getId(),
                "payment-status-changed",
                PaymentStatusChangedEvent.class.getSimpleName(),
                serialize(event)
        ));
        return savedPayment;
    }

    private String serialize(PaymentStatusChangedEvent event) {
        try {
            return objectMapper.writeValueAsString(event);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Failed to serialize payment status event", exception);
        }
    }
}
