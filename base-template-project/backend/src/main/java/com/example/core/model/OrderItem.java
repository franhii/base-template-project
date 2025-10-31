package com.example.core.model;

import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

@Data
@Entity
@Table(name = "order_items")
public class OrderItem {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "item_id", nullable = false)
    private Item item;

    @Column(nullable = false)
    private Integer quantity;

    @Column(nullable = false)
    private BigDecimal priceAtPurchase; // Precio al momento de la compra

    private String itemName; // Guardar nombre por si se elimina el item
    private String itemType; // "PRODUCT" o "SERVICE"

    // ===== CAMPOS PARA SERVICIOS CON BOOKING =====
    private LocalDate bookingDate; // Fecha del turno (solo para servicios)
    private LocalTime bookingTime; // Hora del turno (solo para servicios)
}