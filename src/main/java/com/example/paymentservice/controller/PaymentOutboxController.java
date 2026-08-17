package com.example.paymentservice.controller;

import com.example.paymentservice.outbox.PaymentOutboxEvent;
import com.example.paymentservice.service.PaymentOutboxService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/payment-outbox")
public class PaymentOutboxController {

    private final PaymentOutboxService paymentOutboxService;

    public PaymentOutboxController(PaymentOutboxService paymentOutboxService) {
        this.paymentOutboxService = paymentOutboxService;
    }

    @GetMapping("/dead-lettered")
    public Page<PaymentOutboxEvent> getDeadLetteredEvents(Pageable pageable) {
        return paymentOutboxService.getDeadLetteredEvents(pageable);
    }

    @PostMapping("/{eventId}/requeue")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void requeueDeadLetteredEvent(@PathVariable UUID eventId) {
        paymentOutboxService.requeueDeadLetteredEvent(eventId);
    }
}
