package com.example.core.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class ItemDTO {
    private String id;
    private String name;
    private String description;
    private BigDecimal price;
    private String category;
    private String imageUrl;
    private boolean active;
    private String itemType; // "PRODUCT" o "SERVICE"
    private LocalDateTime createdAt;
}