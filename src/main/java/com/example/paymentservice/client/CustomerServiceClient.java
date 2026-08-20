package com.example.paymentservice.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizeRequest;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;

import java.util.UUID;

@Component
public class CustomerServiceClient implements CustomerContactClient {

    private final RestClient restClient;
    private final OAuth2AuthorizedClientManager authorizedClientManager;

    public CustomerServiceClient(
            RestClient.Builder restClientBuilder,
            OAuth2AuthorizedClientManager authorizedClientManager,
            @Value("${customer.service.url:http://localhost:8085}") String customerServiceUrl
    ) {
        this.restClient = restClientBuilder.baseUrl(customerServiceUrl).build();
        this.authorizedClientManager = authorizedClientManager;
    }

    @Override
    public String paymentPhoneNumber(UUID customerId) {
        var authorizedClient = authorizedClientManager.authorize(OAuth2AuthorizeRequest
                .withClientRegistrationId("payment-service")
                .principal("payment-service")
                .build());
        if (authorizedClient == null || authorizedClient.getAccessToken() == null) {
            throw new IllegalStateException("Unable to obtain payment-service access token");
        }
        PaymentContactResponse response = restClient.get()
                .uri("/api/customers/internal/{customerId}/payment-contact", customerId)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + authorizedClient.getAccessToken().getTokenValue())
                .retrieve()
                .body(PaymentContactResponse.class);
        if (response == null || response.phoneNumber() == null || response.phoneNumber().isBlank()) {
            throw new IllegalStateException("Customer payment contact not found");
        }
        return response.phoneNumber();
    }

    private record PaymentContactResponse(String phoneNumber) {
    }
}
