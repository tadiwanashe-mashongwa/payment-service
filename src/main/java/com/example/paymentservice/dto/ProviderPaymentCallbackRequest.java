package com.example.paymentservice.dto;

import com.example.paymentservice.entity.PaymentStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ProviderPaymentCallbackRequest(
        @NotBlank String providerReference,
        @NotNull PaymentStatus status
) {
}
