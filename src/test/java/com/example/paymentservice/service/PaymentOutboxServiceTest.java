package com.example.paymentservice.service;

import com.example.paymentservice.outbox.PaymentOutboxEvent;
import com.example.paymentservice.outbox.PaymentOutboxEventRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.PageImpl;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentOutboxServiceTest {

    @Mock
    private PaymentOutboxEventRepository paymentOutboxEventRepository;

    @Test
    void shouldRequeueDeadLetteredPaymentOutboxEvent() {
        UUID paymentId = UUID.randomUUID();
        PaymentOutboxEvent event = new PaymentOutboxEvent(
                paymentId, "payment-status-changed", "PaymentStatusChangedEvent", "{}"
        );
        event.recordFailure(new IllegalStateException("Kafka unavailable"));
        event.recordFailure(new IllegalStateException("Kafka unavailable"));
        event.recordFailure(new IllegalStateException("Kafka unavailable"));
        when(paymentOutboxEventRepository.findById(event.getId())).thenReturn(Optional.of(event));
        PaymentOutboxService service = new PaymentOutboxService(paymentOutboxEventRepository);

        service.requeueDeadLetteredEvent(event.getId());

        assertThat(event.isDeadLettered()).isFalse();
        assertThat(event.getAttemptCount()).isZero();
        assertThat(event.getLastError()).isNull();
    }

    @Test
    void shouldListDeadLetteredPaymentOutboxEvents() {
        when(paymentOutboxEventRepository.findByDeadLetteredTrue(PageRequest.of(0, 10)))
                .thenReturn(new PageImpl<>(List.of()));
        PaymentOutboxService service = new PaymentOutboxService(paymentOutboxEventRepository);

        assertThat(service.getDeadLetteredEvents(PageRequest.of(0, 10)).getContent()).isEmpty();
    }
}
