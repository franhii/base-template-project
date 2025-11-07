package com.example.core.dto;

import com.example.core.model.TenantConfig;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class TenantDTO {
    private String id;
    private String subdomain;
    private String businessName;
    private String type; // "GYM", "RETAIL", etc.
    private TenantConfig config;
    private boolean active;
    private LocalDateTime createdAt;
}