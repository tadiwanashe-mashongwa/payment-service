package com.example.paymentservice.provider;

import java.math.BigDecimal;
import java.util.UUID;

public record MobileMoneyChargeRequest(
        UUID orderId,
        UUID customerId,
        String phoneNumber,
        BigDecimal amount
) {
}
