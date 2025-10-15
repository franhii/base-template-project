package com.example.core.dto;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class OrderItemDTO {
    private String id;
    private String itemId;
    private String itemName;
    private String itemType;
    private Integer quantity;
    private BigDecimal priceAtPurchase;
    private BigDecimal subtotal; // quantity * price
}