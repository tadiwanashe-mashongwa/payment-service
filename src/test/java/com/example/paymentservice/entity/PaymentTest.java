package com.example.paymentservice.entity;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PaymentTest {

    @Test
    void shouldAllowPendingPaymentToSucceed() {
        Payment payment = new Payment(UUID.randomUUID(), UUID.randomUUID(), new BigDecimal("19.99"));

        payment.transitionTo(PaymentStatus.SUCCESS);

        assertEquals(PaymentStatus.SUCCESS, payment.getStatus());
    }

    @Test
    void shouldAllowSuccessfulPaymentToBeRefunded() {
        Payment payment = new Payment(UUID.randomUUID(), UUID.randomUUID(), new BigDecimal("19.99"));
        payment.transitionTo(PaymentStatus.SUCCESS);

        payment.transitionTo(PaymentStatus.REFUNDED);

        assertEquals(PaymentStatus.REFUNDED, payment.getStatus());
    }

    @Test
    void shouldRejectRefundForPendingPayment() {
        Payment payment = new Payment(UUID.randomUUID(), UUID.randomUUID(), new BigDecimal("19.99"));

        assertThrows(IllegalStateException.class, () -> payment.transitionTo(PaymentStatus.REFUNDED));
    }
}
