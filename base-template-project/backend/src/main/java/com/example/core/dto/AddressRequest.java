package com.example.core.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import jakarta.validation.constraints.NotBlank;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AddressRequest {

    @NotBlank(message = "La calle es obligatoria")
    private String street;

    @NotBlank(message = "El número de calle es obligatorio")
    private String streetNumber;

    // Datos de GeoRef API
    @NotBlank(message = "El ID de provincia es obligatorio")
    private String provinceId;

    @NotBlank(message = "El nombre de provincia es obligatorio")
    private String provinceName;

    private String municipalityId; // Puede ser null si solo se elige provincia

    @NotBlank(message = "El nombre del municipio es obligatorio")
    private String municipalityName;

    private String localityId; // Puede ser null

    private String localityName;

    @NotBlank(message = "El código postal es obligatorio")
    private String postalCode; // 4 dígitos Argentina

    private String apartment;

    private String reference;

    // Coordenadas opcionales (si el frontend las envía)
    private Double latitude;
    private Double longitude;

    private Boolean isDefault; // Si es null, se toma como false
}