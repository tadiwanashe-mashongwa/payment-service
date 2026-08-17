package com.example.paymentservice.outbox;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

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
    @Scheduled(fixedDelayString = "${payment.outbox.relay.fixed-delay:1000}")
    public void relayPendingEvents() {
        paymentOutboxEventRepository
                .findTop100ByPublishedFalseAndDeadLetteredFalseAndNextAttemptAtLessThanEqualOrderByCreatedAtAsc(
                        Instant.now()
                )
                .forEach(this::publish);
    }

    private void publish(PaymentOutboxEvent event) {
        try {
            kafkaTemplate.send(event.getTopic(), event.getAggregateId().toString(), event.getPayload()).join();
            event.markPublished();
            paymentOutboxEventRepository.save(event);
        } catch (RuntimeException exception) {
            event.recordFailure(exception);
        }
    }
}
