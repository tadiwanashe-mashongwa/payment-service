package com.example.paymentservice.config;

import com.example.paymentservice.controller.PaymentController;
import com.example.paymentservice.service.PaymentService;
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

@WebMvcTest(PaymentController.class)
@Import(SecurityConfig.class)
class SecurityConfigTest {
    @Autowired MockMvc mockMvc;
    @MockitoBean PaymentService paymentService;
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
}
