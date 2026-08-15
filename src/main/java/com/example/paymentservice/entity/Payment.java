package com.example.paymentservice.entity;
import java.math.BigDecimal;
import java.util.UUID;
public class Payment {
    private PaymentStatus status = PaymentStatus.PENDING;
    public Payment(UUID orderId, UUID customerId, BigDecimal amount) { }
    public void transitionTo(PaymentStatus target) {
        if (!((status == PaymentStatus.PENDING && (target == PaymentStatus.SUCCESS || target == PaymentStatus.FAILED)) || (status == PaymentStatus.SUCCESS && target == PaymentStatus.REFUNDED))) {
            throw new IllegalStateException("Invalid payment status transition");
        }
        status = target;
    }
    public PaymentStatus getStatus() { return status; }
}
