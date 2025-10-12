package com.example.core.repository;

import com.example.core.model.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductRepository extends JpaRepository<Product, String> {
    List<Product> findByActiveTrue();
    List<Product> findByCategoryAndActiveTrue(String category);
    List<Product> findByStockGreaterThan(Integer stock);
}