package com.example.core.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class GeoRefLocality {
    private String id;
    private String nombre;

    @JsonProperty("nombre_completo")
    private String nombreCompleto;

    private GeoRefProvince provincia;
    private GeoRefMunicipality municipio;
    private GeoRefCentroid centroide;
}
