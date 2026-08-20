package com.example.paymentservice.controller;

import com.example.paymentservice.dto.CreatePaymentRequest;
import com.example.paymentservice.dto.PaymentResponse;
import com.example.paymentservice.dto.UpdatePaymentStatusRequest;
import com.example.paymentservice.dto.ProviderPaymentCallbackRequest;
import com.example.paymentservice.entity.Payment;
import com.example.paymentservice.service.PaymentService;
import com.example.paymentservice.service.PaymentCallbackAuthenticator;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.UUID;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {

    private final PaymentService paymentService;
    private final PaymentCallbackAuthenticator paymentCallbackAuthenticator;

    public PaymentController(PaymentService paymentService, PaymentCallbackAuthenticator paymentCallbackAuthenticator) {
        this.paymentService = paymentService;
        this.paymentCallbackAuthenticator = paymentCallbackAuthenticator;
    }

    @PostMapping
    public ResponseEntity<PaymentResponse> createPayment(@Valid @RequestBody CreatePaymentRequest request) {
        Payment payment = paymentService.initiatePayment(
                request.orderId(),
                request.customerId(),
                request.amount()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(PaymentResponse.from(payment));
    }

    @GetMapping("/{paymentId}")
    public PaymentResponse getPayment(
            @PathVariable UUID paymentId,
            @AuthenticationPrincipal Jwt jwt,
            Authentication authentication
    ) {
        Payment payment = paymentService.getPayment(paymentId);
        ensureCustomerOwnsPayment(payment.getCustomerId(), jwt, authentication);
        return PaymentResponse.from(payment);
    }

    @GetMapping("/customer/{customerId}")
    public Page<PaymentResponse> getPaymentsByCustomer(
            @PathVariable UUID customerId,
            Pageable pageable,
            @AuthenticationPrincipal Jwt jwt,
            Authentication authentication
    ) {
        ensureCustomerOwnsPayment(customerId, jwt, authentication);
        return paymentService.getPaymentsByCustomer(customerId, pageable);
    }

    @PatchMapping("/{paymentId}/status")
    public PaymentResponse updatePaymentStatus(
            @PathVariable UUID paymentId,
            @Valid @RequestBody UpdatePaymentStatusRequest request
    ) {
        return PaymentResponse.from(paymentService.transitionPaymentStatus(paymentId, request.status()));
    }

    @PostMapping("/callbacks/provider")
    public PaymentResponse handleProviderCallback(
            @RequestBody @Valid ProviderPaymentCallbackRequest request,
            @org.springframework.web.bind.annotation.RequestHeader("X-Payment-Callback-Secret") String callbackSecret
    ) {
        paymentCallbackAuthenticator.assertAuthorized(callbackSecret);
        return PaymentResponse.from(paymentService.handleProviderCallback(request.providerReference(), request.status()));
    }

    private void ensureCustomerOwnsPayment(UUID customerId, Jwt jwt, Authentication authentication) {
        if (authentication.getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals("ROLE_ADMIN"))) {
            return;
        }
        if (jwt != null && customerId.toString().equals(jwt.getSubject())) {
            return;
        }
        throw new AccessDeniedException("Customers can only access their own payments");
    }
}
