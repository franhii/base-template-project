package com.example.core.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.Set;

@Data
@EqualsAndHashCode(callSuper = true)
public class ServiceDTO extends ItemDTO {
    private Integer durationMinutes;
    private String scheduleType;
    private Integer maxCapacity;
    private boolean requiresBooking;

    // Campos de configuración de booking
    private Set<DayOfWeek> availableDays;
    private LocalTime workStartTime;
    private LocalTime workEndTime;
    private Integer slotIntervalMinutes;

    // ✅ NUEVO: Para que super admin pueda asignar a un tenant específico
    private String tenantId;
}