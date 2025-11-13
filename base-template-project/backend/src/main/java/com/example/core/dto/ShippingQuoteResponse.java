package com.example.core.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ShippingQuoteResponse {
    private List<ShippingOptionDTO> options;
    private String originPostalCode;
    private String destinationPostalCode;
}
