package com.example.core.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class ServiceDTO extends ItemDTO {
    private Integer durationMinutes;
    private String scheduleType;
    private Integer maxCapacity;
    private boolean requiresBooking;
}