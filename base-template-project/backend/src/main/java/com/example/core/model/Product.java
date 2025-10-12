package com.example.core.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "products")
public class Product extends Item {

    @Column(nullable = false)
    private Integer stock = 0;

    private String sku;

    private Double weight; // para delivery futuro

    @Enumerated(EnumType.STRING)
    private ProductType type = ProductType.PHYSICAL;

    public enum ProductType {
        PHYSICAL,    // requiere stock
        DIGITAL      // sin stock (ej: ebooks)
    }
}