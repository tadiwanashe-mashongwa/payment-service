package com.example.paymentservice.exception;

public class InvalidPaymentStatusTransitionException extends IllegalStateException {

    public InvalidPaymentStatusTransitionException() {
        super("Invalid payment status transition");
    }
}
