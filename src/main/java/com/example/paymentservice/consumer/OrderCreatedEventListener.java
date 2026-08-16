package com.example.paymentservice.consumer;

import com.example.paymentservice.event.OrderCreatedEvent;
import com.example.paymentservice.service.PaymentService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class OrderCreatedEventListener {

    private final PaymentService paymentService;
    private final ObjectMapper objectMapper;

    public OrderCreatedEventListener(PaymentService paymentService, ObjectMapper objectMapper) {
        this.paymentService = paymentService;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(topics = "order-created", groupId = "payment-service")
    public void handleOrderCreated(String payload) {
        try {
            OrderCreatedEvent event = objectMapper.readValue(payload, OrderCreatedEvent.class);
            paymentService.initiatePaymentForOrder(
                    event.orderId(),
                    event.customerId(),
                    event.totalAmount()
            );
        } catch (JsonProcessingException exception) {
            throw new IllegalArgumentException("Invalid order-created event", exception);
        }
    }
}
