package com.example.core.repository;

import com.example.core.model.ServiceItem;
import com.example.core.model.Tenant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ServiceRepository extends JpaRepository<ServiceItem, String> {
    List<ServiceItem> findByActiveTrue();
    List<ServiceItem> findByActiveTrueAndTenant(Tenant tenant); // ← AGREGAR
    List<ServiceItem> findByCategoryAndActiveTrue(String category);
    List<ServiceItem> findByCategoryAndActiveTrueAndTenant(String category, Tenant tenant); // ← AGREGAR
}