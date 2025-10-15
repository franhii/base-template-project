package com.example.core.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class OrderDTO {
    private String id;
    private String userId;
    private String userName;
    private String tenantId;
    private List<OrderItemDTO> items;
    private BigDecimal total;
    private String status;
    private String paymentMethod;
    private String notes;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}