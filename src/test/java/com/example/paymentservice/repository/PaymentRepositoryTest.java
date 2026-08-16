package com.example.paymentservice.repository;

import com.example.paymentservice.entity.Payment;
import com.example.paymentservice.entity.PaymentStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class PaymentRepositoryTest {

    @Container
    static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine");

    @Autowired
    private PaymentRepository paymentRepository;

    @DynamicPropertySource
    static void configureDatabase(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
    }

    @Test
    void shouldPersistPendingPayment() {
        UUID orderId = UUID.randomUUID();
        Payment payment = new Payment(orderId, UUID.randomUUID(), new BigDecimal("19.99"));

        Payment persisted = paymentRepository.save(payment);

        assertThat(paymentRepository.findById(persisted.getId()))
                .get()
                .satisfies(found -> {
                    assertThat(found.getOrderId()).isEqualTo(orderId);
                    assertThat(found.getStatus()).isEqualTo(PaymentStatus.PENDING);
                });
    }

    @Test
    void shouldFindPaymentsByCustomerWithPagination() {
        UUID customerId = UUID.randomUUID();
        paymentRepository.save(new Payment(UUID.randomUUID(), customerId, new BigDecimal("19.99")));
        paymentRepository.save(new Payment(UUID.randomUUID(), customerId, new BigDecimal("29.99")));
        paymentRepository.save(new Payment(UUID.randomUUID(), UUID.randomUUID(), new BigDecimal("39.99")));

        Page<Payment> result = paymentRepository.findByCustomerId(customerId, PageRequest.of(0, 1));

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getTotalElements()).isEqualTo(2);
        assertThat(result.getContent()).allSatisfy(payment ->
                assertThat(payment.getCustomerId()).isEqualTo(customerId));
    }

    @Test
    void shouldFindPaymentByOrderId() {
        UUID orderId = UUID.randomUUID();
        Payment persisted = paymentRepository.save(
                new Payment(orderId, UUID.randomUUID(), new BigDecimal("42.50"))
        );

        assertThat(paymentRepository.findByOrderId(orderId)).contains(persisted);
    }

    @Test
    void shouldRejectDuplicatePaymentsForTheSameOrder() {
        UUID orderId = UUID.randomUUID();
        paymentRepository.saveAndFlush(new Payment(orderId, UUID.randomUUID(), new BigDecimal("42.50")));

        assertThatThrownBy(() -> paymentRepository.saveAndFlush(
                new Payment(orderId, UUID.randomUUID(), new BigDecimal("42.50"))
        )).isInstanceOf(Exception.class);
    }
}
