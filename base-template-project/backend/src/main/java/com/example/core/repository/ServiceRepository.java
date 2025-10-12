package com.example.core.repository;

import com.example.core.model.ServiceItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ServiceRepository extends JpaRepository<ServiceItem, String> {
    List<ServiceItem> findByActiveTrue();
    List<ServiceItem> findByCategoryAndActiveTrue(String category);
}