package com.example.paymentservice.repository;
import com.example.paymentservice.entity.Payment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;
import java.util.Optional;
public interface PaymentRepository extends JpaRepository<Payment, UUID> {
    Page<Payment> findByCustomerId(UUID customerId, Pageable pageable);
    Optional<Payment> findByOrderId(UUID orderId);
}
