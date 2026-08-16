package com.example.paymentservice.outbox;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
class PaymentOutboxRelayTest {

    @Mock
    private PaymentOutboxEventRepository paymentOutboxEventRepository;

    @Mock
    private KafkaTemplate<String, String> kafkaTemplate;

    @Test
    void shouldPublishPendingPaymentStatusEventAndMarkItPublished() {
        UUID paymentId = UUID.randomUUID();
        PaymentOutboxEvent event = new PaymentOutboxEvent(
                paymentId,
                "payment-status-changed",
                "PaymentStatusChangedEvent",
                "{\"paymentId\":\"%s\"}".formatted(paymentId)
        );
        when(paymentOutboxEventRepository.findByPublishedFalseOrderByCreatedAtAsc())
                .thenReturn(List.of(event));
        when(kafkaTemplate.send(event.getTopic(), paymentId.toString(), event.getPayload()))
                .thenReturn(CompletableFuture.completedFuture(null));
        PaymentOutboxRelay relay = new PaymentOutboxRelay(paymentOutboxEventRepository, kafkaTemplate);

        relay.relayPendingEvents();

        verify(kafkaTemplate).send(event.getTopic(), paymentId.toString(), event.getPayload());
        verify(paymentOutboxEventRepository).save(event);
        assertThat(event.isPublished()).isTrue();
    }

    @Test
    void shouldLeaveEventUnpublishedWhenKafkaSendFails() {
        UUID paymentId = UUID.randomUUID();
        PaymentOutboxEvent event = new PaymentOutboxEvent(
                paymentId,
                "payment-status-changed",
                "PaymentStatusChangedEvent",
                "{\"paymentId\":\"%s\"}".formatted(paymentId)
        );
        when(paymentOutboxEventRepository.findByPublishedFalseOrderByCreatedAtAsc())
                .thenReturn(List.of(event));
        when(kafkaTemplate.send(event.getTopic(), paymentId.toString(), event.getPayload()))
                .thenReturn(CompletableFuture.failedFuture(new IllegalStateException("Kafka unavailable")));
        PaymentOutboxRelay relay = new PaymentOutboxRelay(paymentOutboxEventRepository, kafkaTemplate);

        assertThatThrownBy(relay::relayPendingEvents)
                .isInstanceOf(java.util.concurrent.CompletionException.class);

        verify(paymentOutboxEventRepository, never()).save(event);
        assertThat(event.isPublished()).isFalse();
    }

    @Test
    void shouldScheduleOutboxRelayAtConfiguredFixedDelay() throws NoSuchMethodException {
        Scheduled scheduled = PaymentOutboxRelay.class
                .getMethod("relayPendingEvents")
                .getAnnotation(Scheduled.class);

        assertThat(scheduled).isNotNull();
        assertThat(scheduled.fixedDelayString())
                .isEqualTo("${payment.outbox.relay.fixed-delay:1000}");
    }
}
