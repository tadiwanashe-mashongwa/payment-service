package com.example.paymentservice.outbox;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class PaymentOutboxRelay {

    private final PaymentOutboxEventRepository paymentOutboxEventRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;

    public PaymentOutboxRelay(
            PaymentOutboxEventRepository paymentOutboxEventRepository,
            KafkaTemplate<String, String> kafkaTemplate
    ) {
        this.paymentOutboxEventRepository = paymentOutboxEventRepository;
        this.kafkaTemplate = kafkaTemplate;
    }

    @Transactional
    public void relayPendingEvents() {
        paymentOutboxEventRepository.findByPublishedFalseOrderByCreatedAtAsc()
                .forEach(this::publish);
    }

    private void publish(PaymentOutboxEvent event) {
        kafkaTemplate.send(event.getTopic(), event.getAggregateId().toString(), event.getPayload()).join();
        event.markPublished();
        paymentOutboxEventRepository.save(event);
    }
}
