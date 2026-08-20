package com.example.paymentservice.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

@Component
public class PaymentCallbackAuthenticator {

    private final byte[] expectedSecret;

    public PaymentCallbackAuthenticator(@Value("${payment.callback.secret}") String callbackSecret) {
        this.expectedSecret = callbackSecret.getBytes(StandardCharsets.UTF_8);
    }

    public void assertAuthorized(String providedSecret) {
        byte[] supplied = providedSecret == null ? new byte[0] : providedSecret.getBytes(StandardCharsets.UTF_8);
        if (!MessageDigest.isEqual(expectedSecret, supplied)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid payment callback credentials");
        }
    }
}
