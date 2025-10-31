package com.example.core.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class CreateOrderRequest {

    @NotEmpty
    private List<OrderItemRequest> items;

    @NotNull
    private String paymentMethod; // "CASH", "MERCADO_PAGO", etc.

    private String notes;

    @Data
    public static class OrderItemRequest {
        @NotNull
        private String itemId;

        @NotNull
        private Integer quantity;

        // Para servicios con booking
        private String bookingDate; // formato: "2024-11-15"
        private String bookingTime; // formato: "14:00"
    }
}