package com.example.paymentservice.event;

import com.example.paymentservice.entity.PaymentStatus;

import java.util.UUID;

public record PaymentStatusChangedEvent(UUID paymentId, UUID orderId, PaymentStatus status) {
}
