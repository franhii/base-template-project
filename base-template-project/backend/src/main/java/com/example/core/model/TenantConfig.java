package com.example.core.model;

import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class TenantConfig {
    private String logo;
    private String primaryColor;
    private String secondaryColor;
    private List<String> categories;
    private Map<String, Object> features; // { "delivery": true, "booking": false }
}
