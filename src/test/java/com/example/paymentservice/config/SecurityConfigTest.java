package com.example.paymentservice.config;

import com.example.paymentservice.controller.PaymentController;
import com.example.paymentservice.controller.PaymentOutboxController;
import com.example.paymentservice.service.PaymentService;
import com.example.paymentservice.service.PaymentOutboxService;
import com.example.paymentservice.exception.PaymentNotFoundException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.any;
import org.springframework.data.domain.PageImpl;
import java.util.List;

@WebMvcTest({PaymentController.class, PaymentOutboxController.class})
@Import(SecurityConfig.class)
class SecurityConfigTest {
    @Autowired MockMvc mockMvc;
    @MockitoBean PaymentService paymentService;
    @MockitoBean PaymentOutboxService paymentOutboxService;
    @MockitoBean JwtDecoder jwtDecoder;

    @Test void shouldRejectUnauthenticatedPaymentLookup() throws Exception {
        mockMvc.perform(get("/api/payments/" + java.util.UUID.randomUUID())).andExpect(status().isUnauthorized());
    }

    @Test void shouldAllowCustomerToReadPayment() throws Exception {
        java.util.UUID paymentId = java.util.UUID.randomUUID();
        when(paymentService.getPayment(paymentId)).thenThrow(new PaymentNotFoundException(paymentId));
        mockMvc.perform(get("/api/payments/" + paymentId)
                .with(jwt().authorities(AuthorityUtils.createAuthorityList("ROLE_CUSTOMER")))).andExpect(status().isNotFound());
    }

    @Test void shouldRejectCustomerPaymentCreation() throws Exception {
        mockMvc.perform(post("/api/payments").with(jwt().authorities(AuthorityUtils.createAuthorityList("ROLE_CUSTOMER"))))
                .andExpect(status().isForbidden());
    }

    @Test void shouldPermitOpenApiDocumentation() throws Exception {
        mockMvc.perform(get("/v3/api-docs")).andExpect(status().isNotFound());
    }

    @Test void shouldRejectUnauthenticatedPaymentOutboxLookup() throws Exception {
        mockMvc.perform(get("/api/payment-outbox/dead-lettered")).andExpect(status().isUnauthorized());
    }

    @Test void shouldRejectCustomerPaymentOutboxLookup() throws Exception {
        mockMvc.perform(get("/api/payment-outbox/dead-lettered")
                .with(jwt().authorities(AuthorityUtils.createAuthorityList("ROLE_CUSTOMER"))))
                .andExpect(status().isForbidden());
    }

    @Test void shouldAllowAdminPaymentOutboxLookup() throws Exception {
        when(paymentOutboxService.getDeadLetteredEvents(any())).thenReturn(new PageImpl<>(List.of()));
        mockMvc.perform(get("/api/payment-outbox/dead-lettered")
                .with(jwt().authorities(AuthorityUtils.createAuthorityList("ROLE_ADMIN"))))
                .andExpect(status().isOk());
    }
}
