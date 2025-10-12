package com.example.core.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "services")
public class ServiceItem extends Item {

    @Column(nullable = false)
    private Integer durationMinutes; // duración en minutos

    @Enumerated(EnumType.STRING)
    private ScheduleType scheduleType = ScheduleType.ON_DEMAND;

    private Integer maxCapacity; // para clases grupales

    private boolean requiresBooking = false;

    public enum ScheduleType {
        ON_DEMAND,      // sin agenda (ej: consulta cuando quieras)
        SCHEDULED,      // con turnos específicos (ej: peluquería)
        RECURRING       // planes mensuales (ej: gimnasio)
    }
}