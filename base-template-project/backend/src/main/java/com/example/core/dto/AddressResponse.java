package com.example.core.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AddressResponse {

    private String id;
    private String street;
    private String streetNumber;
    // Datos normalizados GeoRef
    private String provinceId;
    private String provinceName;
    private String municipalityId;
    private String municipalityName;
    private String localityId;
    private String localityName;

    private String postalCode;
    private String apartment;
    private String reference;

    private Double latitude;
    private Double longitude;

    private boolean isDefault;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Método helper para formato completo
    public String getFullAddress() {
        StringBuilder sb = new StringBuilder();
        sb.append(street).append(" ").append(streetNumber);

        if (apartment != null && !apartment.isBlank()) {
            sb.append(", ").append(apartment);
        }

        if (localityName != null && !localityName.isBlank()) {
            sb.append(", ").append(localityName);
        }

        sb.append(", ").append(municipalityName);
        sb.append(", ").append(provinceName);
        sb.append(" (CP: ").append(postalCode).append(")");

        return sb.toString();
    }
}