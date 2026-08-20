package com.example.paymentservice.provider;

public interface MobileMoneyProvider {

    String initiateCharge(MobileMoneyChargeRequest request);
}
