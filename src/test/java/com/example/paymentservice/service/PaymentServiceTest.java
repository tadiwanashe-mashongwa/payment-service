package com.example.paymentservice.service;

import com.example.paymentservice.entity.Payment;
import com.example.paymentservice.entity.PaymentStatus;
import com.example.paymentservice.exception.PaymentNotFoundException;
import com.example.paymentservice.exception.InvalidPaymentStatusTransitionException;
import com.example.paymentservice.repository.PaymentRepository;
import com.example.paymentservice.outbox.PaymentOutboxEvent;
import com.example.paymentservice.outbox.PaymentOutboxEventRepository;
import com.example.paymentservice.event.PaymentStatusChangedEvent;
import com.example.paymentservice.client.CustomerContactClient;
import com.example.paymentservice.provider.MobileMoneyChargeRequest;
import com.example.paymentservice.provider.MobileMoneyProvider;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.times;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private PaymentOutboxEventRepository paymentOutboxEventRepository;

    @Mock
    private CustomerContactClient customerContactClient;

    @Mock
    private MobileMoneyProvider mobileMoneyProvider;

    @Test
    void shouldCreatePendingPayment() {
        UUID orderId = UUID.randomUUID();
        UUID customerId = UUID.randomUUID();
        when(paymentRepository.findByOrderId(orderId)).thenReturn(Optional.empty());
        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(customerContactClient.paymentPhoneNumber(customerId)).thenReturn("+263771000000");
        when(mobileMoneyProvider.initiateCharge(any(MobileMoneyChargeRequest.class))).thenReturn("SIM-default");
        PaymentService paymentService = paymentService();

        Payment result = paymentService.initiatePayment(orderId, customerId, new BigDecimal("19.99"));

        ArgumentCaptor<Payment> paymentCaptor = ArgumentCaptor.forClass(Payment.class);
        verify(paymentRepository, times(2)).save(paymentCaptor.capture());
        assertThat(paymentCaptor.getValue().getStatus()).isEqualTo(PaymentStatus.PENDING);
        assertThat(result).isSameAs(paymentCaptor.getValue());
    }

    @Test
    void shouldInitiateMobileMoneyChargeAndStoreProviderReferenceForNewPayment() {
        UUID orderId = UUID.randomUUID();
        UUID customerId = UUID.randomUUID();
        when(paymentRepository.findByOrderId(orderId)).thenReturn(Optional.empty());
        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(customerContactClient.paymentPhoneNumber(customerId)).thenReturn("+263771000000");
        when(mobileMoneyProvider.initiateCharge(any(MobileMoneyChargeRequest.class))).thenReturn("SIM-12345");
        PaymentService paymentService = paymentService();

        Payment payment = paymentService.initiatePaymentForOrder(orderId, customerId, new BigDecimal("42.50"));

        assertThat(payment.getProviderReference()).isEqualTo("SIM-12345");
        verify(customerContactClient).paymentPhoneNumber(customerId);
        verify(mobileMoneyProvider).initiateCharge(new MobileMoneyChargeRequest(
                orderId, customerId, "+263771000000", new BigDecimal("42.50")
        ));
        verify(paymentRepository, times(2)).save(payment);
    }

    @Test
    void shouldReuseExistingPaymentForDuplicateDirectCreation() {
        UUID orderId = UUID.randomUUID();
        Payment existing = new Payment(orderId, UUID.randomUUID(), new BigDecimal("19.99"));
        when(paymentRepository.findByOrderId(orderId)).thenReturn(Optional.of(existing));
        PaymentService paymentService = paymentService();

        Payment result = paymentService.initiatePayment(orderId, UUID.randomUUID(), new BigDecimal("19.99"));

        assertThat(result).isSameAs(existing);
        verify(paymentRepository, never()).save(any(Payment.class));
    }

    @Test
    void shouldReturnPaymentById() {
        UUID paymentId = UUID.randomUUID();
        Payment payment = new Payment(UUID.randomUUID(), UUID.randomUUID(), new BigDecimal("19.99"));
        when(paymentRepository.findById(paymentId)).thenReturn(Optional.of(payment));
        PaymentService paymentService = paymentService();

        Payment result = paymentService.getPayment(paymentId);

        assertThat(result).isSameAs(payment);
        verify(paymentRepository).findById(paymentId);
    }

    @Test
    void shouldThrowWhenPaymentDoesNotExist() {
        UUID paymentId = UUID.randomUUID();
        when(paymentRepository.findById(paymentId)).thenReturn(Optional.empty());
        PaymentService paymentService = paymentService();

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
        PaymentService paymentService = paymentService();

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
        PaymentService paymentService = paymentService();

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
        PaymentService paymentService = paymentService();

        assertThatThrownBy(() -> paymentService.transitionPaymentStatus(paymentId, PaymentStatus.SUCCESS))
                .isInstanceOf(InvalidPaymentStatusTransitionException.class);
        verify(paymentRepository, never()).save(payment);
    }

    @Test
    void shouldIgnoreDuplicatePaymentStatusTransition() {
        UUID paymentId = UUID.randomUUID();
        Payment payment = new Payment(UUID.randomUUID(), UUID.randomUUID(), new BigDecimal("19.99"));
        payment.transitionTo(PaymentStatus.SUCCESS);
        when(paymentRepository.findById(paymentId)).thenReturn(Optional.of(payment));
        PaymentService paymentService = paymentService();

        Payment result = paymentService.transitionPaymentStatus(paymentId, PaymentStatus.SUCCESS);

        assertThat(result.getStatus()).isEqualTo(PaymentStatus.SUCCESS);
        verify(paymentRepository, never()).save(payment);
        verifyNoInteractions(paymentOutboxEventRepository);
    }

    @Test
    void shouldPersistPaymentStatusChangedEventInOutbox() throws Exception {
        UUID paymentId = UUID.randomUUID();
        Payment payment = new Payment(UUID.randomUUID(), UUID.randomUUID(), new BigDecimal("19.99"));
        Payment savedPayment = org.mockito.Mockito.mock(Payment.class);
        when(paymentRepository.findById(paymentId)).thenReturn(Optional.of(payment));
        when(paymentRepository.save(payment)).thenReturn(savedPayment);
        when(savedPayment.getId()).thenReturn(paymentId);
        when(savedPayment.getOrderId()).thenReturn(payment.getOrderId());
        when(savedPayment.getStatus()).thenReturn(PaymentStatus.SUCCESS);
        PaymentService paymentService = paymentService();

        paymentService.transitionPaymentStatus(paymentId, PaymentStatus.SUCCESS);

        ArgumentCaptor<PaymentOutboxEvent> eventCaptor = ArgumentCaptor.forClass(PaymentOutboxEvent.class);
        verify(paymentOutboxEventRepository).save(eventCaptor.capture());
        PaymentStatusChangedEvent event = new ObjectMapper().readValue(
                eventCaptor.getValue().getPayload(),
                PaymentStatusChangedEvent.class
        );
        assertThat(event.paymentId()).isEqualTo(paymentId);
        assertThat(event.orderId()).isEqualTo(payment.getOrderId());
        assertThat(event.status()).isEqualTo(PaymentStatus.SUCCESS);
        assertThat(eventCaptor.getValue().getTopic()).isEqualTo("payment-status-changed");
    }

    @Test
    void shouldInitiateOnlyOnePaymentForAnOrder() {
        UUID orderId = UUID.randomUUID();
        UUID customerId = UUID.randomUUID();
        when(paymentRepository.findByOrderId(orderId)).thenReturn(Optional.empty());
        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(customerContactClient.paymentPhoneNumber(customerId)).thenReturn("+263771000000");
        when(mobileMoneyProvider.initiateCharge(any(MobileMoneyChargeRequest.class))).thenReturn("SIM-default");
        PaymentService paymentService = paymentService();

        Payment payment = paymentService.initiatePaymentForOrder(orderId, customerId, new BigDecimal("42.50"));

        assertThat(payment.getOrderId()).isEqualTo(orderId);
        assertThat(payment.getCustomerId()).isEqualTo(customerId);
        assertThat(payment.getAmount()).isEqualByComparingTo("42.50");
        verify(paymentRepository, times(2)).save(any(Payment.class));
    }

    @Test
    void shouldReuseExistingPaymentForDuplicateOrderCreatedEvent() {
        UUID orderId = UUID.randomUUID();
        Payment existing = new Payment(orderId, UUID.randomUUID(), new BigDecimal("42.50"));
        when(paymentRepository.findByOrderId(orderId)).thenReturn(Optional.of(existing));
        PaymentService paymentService = paymentService();

        Payment payment = paymentService.initiatePaymentForOrder(orderId, UUID.randomUUID(), new BigDecimal("42.50"));

        assertThat(payment).isSameAs(existing);
        verify(paymentRepository, never()).save(any(Payment.class));
    }
    private PaymentService paymentService() {
        return new PaymentService(
                paymentRepository,
                paymentOutboxEventRepository,
                new ObjectMapper(),
                customerContactClient,
                mobileMoneyProvider
        );
    }
}
