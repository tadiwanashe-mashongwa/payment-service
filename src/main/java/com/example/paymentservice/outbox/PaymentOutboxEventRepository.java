package com.example.paymentservice.outbox;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.time.Instant;
import java.util.UUID;

public interface PaymentOutboxEventRepository extends JpaRepository<PaymentOutboxEvent, UUID> {

    List<PaymentOutboxEvent> findTop100ByPublishedFalseAndDeadLetteredFalseAndNextAttemptAtLessThanEqualOrderByCreatedAtAsc(
            Instant now
    );
}
