package com.example.core.repository;

import com.example.core.model.Product;
import com.example.core.model.Tenant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductRepository extends JpaRepository<Product, String> {
    List<Product> findByActiveTrue();
    List<Product> findByActiveTrueAndTenant(Tenant tenant); // ← AGREGAR
    List<Product> findByCategoryAndActiveTrue(String category);
    List<Product> findByCategoryAndActiveTrueAndTenant(String category, Tenant tenant); // ← AGREGAR
    List<Product> findByStockGreaterThan(Integer stock);
    List<Product> findByStockGreaterThanAndTenant(Integer stock, Tenant tenant); // ← AGREGAR
}