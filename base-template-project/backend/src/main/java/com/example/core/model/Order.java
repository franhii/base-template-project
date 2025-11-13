package com.example.core.model;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@Entity
@Table(name = "orders")
public class Order {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderItem> items = new ArrayList<>();

    @Column(nullable = false)
    private BigDecimal total;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrderStatus status = OrderStatus.PENDING;

    @Enumerated(EnumType.STRING)
    private PaymentMethod paymentMethod;

    private String notes; // Notas del cliente

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "delivery_address_id")
    private Address deliveryAddress;

    @Column(name = "delivery_cost")
    private BigDecimal deliveryCost;

    @Column(name = "is_delivery")
    private boolean isDelivery;

    @Column(name = "delivery_notes")
    private String deliveryNotes;

    // Nuevo para MercadoEnvíos
    @Column(name = "shipping_method_id")
    private Long shippingMethodId; // ID del método de envío de ML

    @Column(name = "shipment_id")
    private String shipmentId; // ID del envío en MercadoLibre (tracking)

    // Métodos helper
    public BigDecimal getTotalWithDelivery() {
        if (deliveryCost == null) {
            return total;
        }
        return total.add(deliveryCost);
    }

    public boolean isFreeDelivery() {
        return isDelivery &&
                deliveryCost != null &&
                deliveryCost.compareTo(BigDecimal.ZERO) == 0;
    }

    public enum OrderStatus {
        PENDING,      // Pendiente de pago
        CONFIRMED,    // Confirmada y pagada
        PREPARING,    // En preparación
        READY,        // Lista para entrega/retiro
        COMPLETED,    // Completada
        CANCELLED,     // Cancelada
        PICKUP_READY // Lista para retiro en tienda
    }

    public enum PaymentMethod {
        CASH,              // Efectivo
        MERCADO_PAGO,      // MercadoPago
        BANK_TRANSFER,     // Transferencia bancaria
        CREDIT_CARD,       // Tarjeta de crédito
        DEBIT_CARD         // Tarjeta de débito
    }
}