package com.example.core.model;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "payments")
public class Payment {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @OneToOne
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentMethod method;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentStatus status = PaymentStatus.PENDING;

    @Column(nullable = false)
    private BigDecimal amount;

    // Para MercadoPago
    private String externalId; // ID de MercadoPago
    private String externalStatus; // Estado de MercadoPago
    private String paymentLink; // Link de pago

    // Para comprobante de transferencia/depósito
    private String receiptUrl; // URL de la imagen del comprobante
    private String receiptNotes; // Notas del comprobante

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    private LocalDateTime confirmedAt;

    public enum PaymentMethod {
        CASH,
        MERCADO_PAGO,
        BANK_TRANSFER,
        CREDIT_CARD,
        DEBIT_CARD
    }

    public enum PaymentStatus {
        PENDING,        // Pendiente
        PROCESSING,     // En proceso (MercadoPago)
        APPROVED,       // Aprobado
        REJECTED,       // Rechazado
        CANCELLED,      // Cancelado
        REFUNDED        // Reembolsado
    }
}