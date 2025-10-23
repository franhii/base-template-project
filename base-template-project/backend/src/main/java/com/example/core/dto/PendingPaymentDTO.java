package com.example.core.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

// ========== PendingPaymentDTO ==========
// Pagos pendientes de revisión
@Data
public class PendingPaymentDTO {
    private String id;
    private String orderId;
    private BigDecimal amount;
    private String method;                    // BANK_TRANSFER, CASH, etc.
    private String status;
    private String receiptUrl;                // URL del comprobante
    private String receiptNotes;              // Notas del comprobante
    private LocalDateTime createdAt;
    private String customerName;
    private String customerEmail;
}