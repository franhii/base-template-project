package com.example.core.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class ShippingOption {

    @JsonProperty("name")
    private String name; // "Estándar a domicilio", "Retiro en sucursal", etc.

    @JsonProperty("cost")
    private BigDecimal cost; // Costo del envío

    @JsonProperty("list_cost")
    private BigDecimal listCost; // Costo original (antes de descuentos)

    @JsonProperty("shipping_method_id")
    private Long shippingMethodId; // ID único del método (100009, 100012, etc.)

    @JsonProperty("estimated_delivery_time")
    private EstimatedDelivery estimatedDelivery;

    @JsonProperty("speed")
    private String speed; // "standard", "express"

    @JsonProperty("display")
    private String display; // Texto para mostrar al usuario

    // Helper para verificar si es gratis
    public boolean isFree() {
        return cost != null && cost.compareTo(BigDecimal.ZERO) == 0;
    }
}