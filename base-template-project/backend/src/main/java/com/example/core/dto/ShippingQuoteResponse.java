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
    List<ShippingOptionDTO> options;
    String originPostalCode;
    String destinationPostalCode;
}
