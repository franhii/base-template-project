package com.example.core.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class PaymentDTO {
    private String id;
    private String orderId;
    private String method;
    private String status;
    private BigDecimal amount;
    private String externalId;
    private String paymentLink;
    private String receiptUrl;
    private String receiptNotes;
    private LocalDateTime createdAt;
    private LocalDateTime confirmedAt;
}