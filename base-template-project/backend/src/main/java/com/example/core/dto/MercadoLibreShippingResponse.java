package com.example.core.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.mercadopago.resources.merchantorder.MerchantOrderShippingOption;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

// ========== RESPONSE DE MERCADOLIBRE ==========
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class MercadoLibreShippingResponse {

    @JsonProperty("options")
    private List<MerchantOrderShippingOption> options;

    @JsonProperty("free_methods")
    private List<Object> freeMethods; // Métodos gratis disponibles
}