package com.example.core.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.HashSet;
import java.util.Set;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "services")
public class ServiceItem extends Item {

    @Column(nullable = false)
    private Integer durationMinutes; // duración en minutos

    @Enumerated(EnumType.STRING)
    private ScheduleType scheduleType = ScheduleType.ON_DEMAND;

    private Integer maxCapacity; // capacidad máxima por turno/clase

    private boolean requiresBooking = false;

    // ===== CONFIGURACIÓN DE DISPONIBILIDAD =====

    @ElementCollection(fetch = FetchType.EAGER)
    @Enumerated(EnumType.STRING)
    @CollectionTable(name = "service_available_days", joinColumns = @JoinColumn(name = "service_id"))
    @Column(name = "day_of_week")
    private Set<DayOfWeek> availableDays = new HashSet<>(); // Días de la semana disponibles

    private LocalTime workStartTime; // Hora de inicio (ej: 09:00)

    private LocalTime workEndTime; // Hora de fin (ej: 18:00)

    private Integer slotIntervalMinutes; // Intervalo entre turnos (ej: 30 minutos)

    public enum ScheduleType {
        ON_DEMAND,      // sin agenda (ej: consulta cuando quieras)
        SCHEDULED,      // con turnos específicos (ej: peluquería)
        RECURRING       // planes mensuales (ej: gimnasio)
    }
}