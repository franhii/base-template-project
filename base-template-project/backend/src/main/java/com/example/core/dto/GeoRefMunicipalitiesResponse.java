package com.example.core.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class GeoRefMunicipalitiesResponse {
    private int cantidad;
    private int total;
    private int inicio;
    
    @JsonProperty("municipios")
    private List<GeoRefMunicipality> municipios;
    
    @JsonProperty("parametros")
    private Object parametros;
}