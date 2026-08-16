package com.example.paymentservice.consumer;

import com.example.paymentservice.service.PaymentService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.UUID;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class OrderCreatedEventListenerTest {

    @Mock
    private PaymentService paymentService;

    @Test
    void shouldInitiatePaymentFromOrderCreatedMessage() throws Exception {
        UUID orderId = UUID.randomUUID();
        UUID customerId = UUID.randomUUID();
        OrderCreatedEventListener listener = new OrderCreatedEventListener(paymentService, new ObjectMapper());

        listener.handleOrderCreated("""
                {
                  "orderId": "%s",
                  "customerId": "%s",
                  "totalAmount": 42.50,
                  "items": []
                }
                """.formatted(orderId, customerId));

        verify(paymentService).initiatePaymentForOrder(orderId, customerId, new BigDecimal("42.50"));
    }
}
