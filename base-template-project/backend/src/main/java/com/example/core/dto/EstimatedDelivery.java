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
public class EstimatedDelivery {

    @JsonProperty("date")
    private String date; // Fecha estimada: "2024-11-20"

    @JsonProperty("shipping")
    private String shipping; // Horas de envío: "24"

    @JsonProperty("handling")
    private String handling; // Horas de preparación: "24"

    @JsonProperty("schedule")
    private List<DeliverySchedule> schedule; // Horarios de entrega
}
