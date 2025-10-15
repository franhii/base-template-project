package com.example.core.repository;

import com.example.core.model.Order;
import com.example.core.model.Tenant;
import com.example.core.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrderRepository extends JpaRepository<Order, String> {
    List<Order> findByUser(User user);
    List<Order> findByUserOrderByCreatedAtDesc(User user);
    List<Order> findByTenant(Tenant tenant);
    List<Order> findByTenantOrderByCreatedAtDesc(Tenant tenant);
    List<Order> findByStatus(Order.OrderStatus status);
}