package com.example.paymentservice.provider;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@ConditionalOnProperty(name = "payment.provider", havingValue = "local", matchIfMissing = true)
public class LocalMobileMoneyProvider implements MobileMoneyProvider {

    @Override
    public String initiateCharge(MobileMoneyChargeRequest request) {
        return "SIM-" + UUID.randomUUID();
    }
}
