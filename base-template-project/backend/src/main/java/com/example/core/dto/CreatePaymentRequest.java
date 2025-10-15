package com.example.core.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CreatePaymentRequest {
    @NotBlank
    private String orderId;

    @NotBlank
    private String method; // "MERCADO_PAGO", "BANK_TRANSFER", "CASH"
}