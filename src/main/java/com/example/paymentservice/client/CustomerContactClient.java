package com.example.paymentservice.client;

import java.util.UUID;

public interface CustomerContactClient {

    String paymentPhoneNumber(UUID customerId);
}
