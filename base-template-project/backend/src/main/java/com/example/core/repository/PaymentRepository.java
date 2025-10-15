package com.example.core.repository;

import com.example.core.model.Payment;
import com.example.core.model.Order;
import com.example.core.model.Tenant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, String> {
    Optional<Payment> findByOrder(Order order);
    Optional<Payment> findByExternalId(String externalId);
    List<Payment> findByTenant(Tenant tenant);
    List<Payment> findByStatus(Payment.PaymentStatus status);
    List<Payment> findByTenantAndStatus(Tenant tenant, Payment.PaymentStatus status);
}