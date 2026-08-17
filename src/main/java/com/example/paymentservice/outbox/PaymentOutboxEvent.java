package com.example.paymentservice.outbox;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UuidGenerator;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "payment_outbox_events")
public class PaymentOutboxEvent {

    @Id
    @UuidGenerator
    private UUID id;

    @Column(nullable = false)
    private UUID aggregateId;

    @Column(nullable = false)
    private String topic;

    @Column(nullable = false)
    private String eventType;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String payload;

    @Column(nullable = false)
    private boolean published;

    @Column(nullable = false)
    private int attemptCount;

    @Column(columnDefinition = "TEXT")
    private String lastError;

    @Column(nullable = false)
    private Instant nextAttemptAt = Instant.now();

    @Column(nullable = false)
    private boolean deadLettered;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    protected PaymentOutboxEvent() {
    }

    public PaymentOutboxEvent(UUID aggregateId, String topic, String eventType, String payload) {
        this.aggregateId = aggregateId;
        this.topic = topic;
        this.eventType = eventType;
        this.payload = payload;
    }

    public String getPayload() { return payload; }
    public String getTopic() { return topic; }
    public UUID getAggregateId() { return aggregateId; }

    public boolean isPublished() { return published; }
    public int getAttemptCount() { return attemptCount; }
    public String getLastError() { return lastError; }
    public Instant getNextAttemptAt() { return nextAttemptAt; }
    public boolean isDeadLettered() { return deadLettered; }

    public void markPublished() {
        this.published = true;
    }

    public void recordFailure(Exception exception) {
        attemptCount++;
        lastError = exception.getCause() == null
                ? exception.getMessage()
                : exception.getCause().getMessage();
        if (attemptCount >= 3) {
            deadLettered = true;
        } else {
            nextAttemptAt = Instant.now().plusSeconds(1L << (attemptCount - 1));
        }
    }
}
