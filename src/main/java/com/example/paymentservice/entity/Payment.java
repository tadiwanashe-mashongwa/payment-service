package com.example.paymentservice.entity;
import com.example.paymentservice.exception.InvalidPaymentStatusTransitionException;
import jakarta.persistence.*;
import org.hibernate.annotations.UuidGenerator;
import java.math.BigDecimal;
import java.util.UUID;
@Entity
@Table(name = "payments")
public class Payment {
    @Id @UuidGenerator private UUID id;
    @Column(nullable = false) private UUID orderId;
    @Column(nullable = false) private UUID customerId;
    @Column(nullable = false) private BigDecimal amount;
    @Enumerated(EnumType.STRING) @Column(nullable = false)
    private PaymentStatus status = PaymentStatus.PENDING;
    protected Payment() { }
    public Payment(UUID orderId, UUID customerId, BigDecimal amount) {
        if (amount == null || amount.signum() <= 0) {
            throw new IllegalArgumentException("Payment amount must be positive");
        }
        this.orderId = orderId;
        this.customerId = customerId;
        this.amount = amount;
    }
    public void transitionTo(PaymentStatus target) {
        if (!((status == PaymentStatus.PENDING && (target == PaymentStatus.SUCCESS || target == PaymentStatus.FAILED)) || (status == PaymentStatus.SUCCESS && target == PaymentStatus.REFUNDED))) {
            throw new InvalidPaymentStatusTransitionException();
        }
        status = target;
    }
    public PaymentStatus getStatus() { return status; }
    public UUID getId() { return id; }
    public UUID getOrderId() { return orderId; }
    public UUID getCustomerId() { return customerId; }
    public BigDecimal getAmount() { return amount; }
}
