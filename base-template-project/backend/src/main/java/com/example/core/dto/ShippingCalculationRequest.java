package com.example.core.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

// ========== REQUEST PARA COTIZAR ENVÍO MERCADOENVIOS ==========
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ShippingCalculationRequest {

    @JsonProperty("dimensions")
    private String dimensions; // Formato: "10x10x20,500" (ancho x alto x largo en cm, peso en gr)

    @JsonProperty("zip_code_from")
    private String zipCodeFrom; // CP origen (del negocio)

    @JsonProperty("zip_code_to")
    private String zipCodeTo; // CP destino (del cliente)

    @JsonProperty("list_cost")
    private BigDecimal listCost; // Precio del producto

    @JsonProperty("free_shipping")
    private Boolean freeShipping; // Si el vendedor ofrece envío gratis
}