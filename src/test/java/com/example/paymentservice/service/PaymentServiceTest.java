package com.example.paymentservice.service;

import com.example.paymentservice.entity.Payment;
import com.example.paymentservice.entity.PaymentStatus;
import com.example.paymentservice.exception.PaymentNotFoundException;
import com.example.paymentservice.exception.InvalidPaymentStatusTransitionException;
import com.example.paymentservice.repository.PaymentRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.UUID;
import java.util.Optional;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import com.example.paymentservice.dto.PaymentResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock
    private PaymentRepository paymentRepository;

    @Test
    void shouldCreatePendingPayment() {
        UUID orderId = UUID.randomUUID();
        UUID customerId = UUID.randomUUID();
        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> invocation.getArgument(0));
        PaymentService paymentService = new PaymentService(paymentRepository);

        Payment result = paymentService.initiatePayment(orderId, customerId, new BigDecimal("19.99"));

        ArgumentCaptor<Payment> paymentCaptor = ArgumentCaptor.forClass(Payment.class);
        verify(paymentRepository).save(paymentCaptor.capture());
        assertThat(paymentCaptor.getValue().getStatus()).isEqualTo(PaymentStatus.PENDING);
        assertThat(result).isSameAs(paymentCaptor.getValue());
    }

    @Test
    void shouldReturnPaymentById() {
        UUID paymentId = UUID.randomUUID();
        Payment payment = new Payment(UUID.randomUUID(), UUID.randomUUID(), new BigDecimal("19.99"));
        when(paymentRepository.findById(paymentId)).thenReturn(Optional.of(payment));
        PaymentService paymentService = new PaymentService(paymentRepository);

        Payment result = paymentService.getPayment(paymentId);

        assertThat(result).isSameAs(payment);
        verify(paymentRepository).findById(paymentId);
    }

    @Test
    void shouldThrowWhenPaymentDoesNotExist() {
        UUID paymentId = UUID.randomUUID();
        when(paymentRepository.findById(paymentId)).thenReturn(Optional.empty());
        PaymentService paymentService = new PaymentService(paymentRepository);

        assertThatThrownBy(() -> paymentService.getPayment(paymentId))
                .isInstanceOf(PaymentNotFoundException.class)
                .hasMessageContaining(paymentId.toString());
    }

    @Test
    void shouldReturnPaginatedPaymentsForCustomer() {
        UUID customerId = UUID.randomUUID();
        PageRequest pageable = PageRequest.of(0, 10);
        Payment payment = new Payment(UUID.randomUUID(), customerId, new BigDecimal("19.99"));
        when(paymentRepository.findByCustomerId(customerId, pageable))
                .thenReturn(new PageImpl<>(List.of(payment), pageable, 1));
        PaymentService paymentService = new PaymentService(paymentRepository);

        Page<PaymentResponse> result = paymentService.getPaymentsByCustomer(customerId, pageable);

        assertThat(result.getContent())
                .singleElement()
                .satisfies(response -> assertThat(response.customerId()).isEqualTo(customerId));
        verify(paymentRepository).findByCustomerId(customerId, pageable);
    }

    @Test
    void shouldTransitionPaymentStatusAndPersistIt() {
        UUID paymentId = UUID.randomUUID();
        Payment payment = new Payment(UUID.randomUUID(), UUID.randomUUID(), new BigDecimal("19.99"));
        when(paymentRepository.findById(paymentId)).thenReturn(Optional.of(payment));
        when(paymentRepository.save(payment)).thenReturn(payment);
        PaymentService paymentService = new PaymentService(paymentRepository);

        Payment result = paymentService.transitionPaymentStatus(paymentId, PaymentStatus.SUCCESS);

        assertThat(result.getStatus()).isEqualTo(PaymentStatus.SUCCESS);
        verify(paymentRepository).save(payment);
    }

    @Test
    void shouldNotPersistInvalidPaymentStatusTransition() {
        UUID paymentId = UUID.randomUUID();
        Payment payment = new Payment(UUID.randomUUID(), UUID.randomUUID(), new BigDecimal("19.99"));
        payment.transitionTo(PaymentStatus.FAILED);
        when(paymentRepository.findById(paymentId)).thenReturn(Optional.of(payment));
        PaymentService paymentService = new PaymentService(paymentRepository);

        assertThatThrownBy(() -> paymentService.transitionPaymentStatus(paymentId, PaymentStatus.SUCCESS))
                .isInstanceOf(InvalidPaymentStatusTransitionException.class);
        verify(paymentRepository, never()).save(payment);
    }
}
