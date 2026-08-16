package com.example.paymentservice.controller;

import com.example.paymentservice.entity.Payment;
import com.example.paymentservice.exception.PaymentNotFoundException;
import com.example.paymentservice.exception.InvalidPaymentStatusTransitionException;
import com.example.paymentservice.dto.PaymentResponse;
import com.example.paymentservice.service.PaymentService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.UUID;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PaymentController.class)
class PaymentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PaymentService paymentService;

    @Test
    void shouldCreatePendingPayment() throws Exception {
        UUID orderId = UUID.randomUUID();
        UUID customerId = UUID.randomUUID();
        UUID paymentId = UUID.randomUUID();
        Payment payment = mock(Payment.class);
        when(payment.getId()).thenReturn(paymentId);
        when(payment.getOrderId()).thenReturn(orderId);
        when(payment.getCustomerId()).thenReturn(customerId);
        when(payment.getAmount()).thenReturn(new BigDecimal("19.99"));
        when(payment.getStatus()).thenReturn(com.example.paymentservice.entity.PaymentStatus.PENDING);
        when(paymentService.initiatePayment(any(), any(), any())).thenReturn(payment);

        mockMvc.perform(post("/api/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "orderId": "%s",
                                  "customerId": "%s",
                                  "amount": 19.99
                                }
                                """.formatted(orderId, customerId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(paymentId.toString()))
                .andExpect(jsonPath("$.orderId").value(orderId.toString()))
                .andExpect(jsonPath("$.customerId").value(customerId.toString()))
                .andExpect(jsonPath("$.amount").value(19.99))
                .andExpect(jsonPath("$.status").value("PENDING"));

        verify(paymentService).initiatePayment(eq(orderId), eq(customerId), eq(new BigDecimal("19.99")));
    }

    @Test
    void shouldRejectNonPositivePaymentAmount() throws Exception {
        UUID orderId = UUID.randomUUID();
        UUID customerId = UUID.randomUUID();

        mockMvc.perform(post("/api/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "orderId": "%s",
                                  "customerId": "%s",
                                  "amount": 0
                                }
                                """.formatted(orderId, customerId)))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(paymentService);
    }

    @Test
    void shouldReturnPaymentById() throws Exception {
        UUID paymentId = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();
        UUID customerId = UUID.randomUUID();
        Payment payment = mock(Payment.class);
        when(payment.getId()).thenReturn(paymentId);
        when(payment.getOrderId()).thenReturn(orderId);
        when(payment.getCustomerId()).thenReturn(customerId);
        when(payment.getAmount()).thenReturn(new BigDecimal("19.99"));
        when(payment.getStatus()).thenReturn(com.example.paymentservice.entity.PaymentStatus.PENDING);
        when(paymentService.getPayment(paymentId)).thenReturn(payment);

        mockMvc.perform(get("/api/payments/{paymentId}", paymentId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(paymentId.toString()))
                .andExpect(jsonPath("$.status").value("PENDING"));

        verify(paymentService).getPayment(paymentId);
    }

    @Test
    void shouldReturnNotFoundWhenPaymentDoesNotExist() throws Exception {
        UUID paymentId = UUID.randomUUID();
        when(paymentService.getPayment(paymentId)).thenThrow(new PaymentNotFoundException(paymentId));

        mockMvc.perform(get("/api/payments/{paymentId}", paymentId))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldReturnPaginatedPaymentsForCustomer() throws Exception {
        UUID customerId = UUID.randomUUID();
        PaymentResponse payment = new PaymentResponse(
                UUID.randomUUID(),
                UUID.randomUUID(),
                customerId,
                new BigDecimal("19.99"),
                com.example.paymentservice.entity.PaymentStatus.PENDING
        );
        when(paymentService.getPaymentsByCustomer(eq(customerId), any()))
                .thenReturn(new PageImpl<>(List.of(payment), PageRequest.of(0, 1), 1));

        mockMvc.perform(get("/api/payments/customer/{customerId}", customerId)
                        .param("page", "0")
                        .param("size", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].customerId").value(customerId.toString()))
                .andExpect(jsonPath("$.content[0].status").value("PENDING"))
                .andExpect(jsonPath("$.totalElements").value(1));

        verify(paymentService).getPaymentsByCustomer(eq(customerId), any());
    }

    @Test
    void shouldUpdatePaymentStatus() throws Exception {
        UUID paymentId = UUID.randomUUID();
        Payment payment = mock(Payment.class);
        when(payment.getId()).thenReturn(paymentId);
        when(payment.getStatus()).thenReturn(com.example.paymentservice.entity.PaymentStatus.SUCCESS);
        when(paymentService.transitionPaymentStatus(
                paymentId,
                com.example.paymentservice.entity.PaymentStatus.SUCCESS
        )).thenReturn(payment);

        mockMvc.perform(patch("/api/payments/{paymentId}/status", paymentId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"SUCCESS\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(paymentId.toString()))
                .andExpect(jsonPath("$.status").value("SUCCESS"));

        verify(paymentService).transitionPaymentStatus(
                paymentId,
                com.example.paymentservice.entity.PaymentStatus.SUCCESS
        );
    }

    @Test
    void shouldRejectInvalidPaymentStatusTransition() throws Exception {
        UUID paymentId = UUID.randomUUID();
        when(paymentService.transitionPaymentStatus(
                paymentId,
                com.example.paymentservice.entity.PaymentStatus.REFUNDED
        )).thenThrow(new InvalidPaymentStatusTransitionException());

        mockMvc.perform(patch("/api/payments/{paymentId}/status", paymentId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"REFUNDED\"}"))
                .andExpect(status().isBadRequest());
    }
}
