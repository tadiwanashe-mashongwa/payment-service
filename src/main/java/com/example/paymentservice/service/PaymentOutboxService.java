package com.example.paymentservice.service;

import com.example.paymentservice.outbox.PaymentOutboxEventRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class PaymentOutboxService {

    private final PaymentOutboxEventRepository paymentOutboxEventRepository;

    public PaymentOutboxService(PaymentOutboxEventRepository paymentOutboxEventRepository) {
        this.paymentOutboxEventRepository = paymentOutboxEventRepository;
    }

    @Transactional
    public void requeueDeadLetteredEvent(UUID eventId) {
        paymentOutboxEventRepository.findById(eventId)
                .orElseThrow(() -> new IllegalArgumentException("Payment outbox event " + eventId + " not found"))
                .requeue();
    }
}
