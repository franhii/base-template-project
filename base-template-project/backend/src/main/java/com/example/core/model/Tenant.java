package com.example.core.model;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;

@Entity
@Table(name = "tenants")
@Data
public class Tenant {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(unique = true, nullable = false)
    private String subdomain;

    @Column(nullable = false)
    private String businessName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BusinessType type;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private TenantConfig config;

    // ✅ NUEVO: Estado activo/suspendido
    @Column(nullable = false)
    private boolean active = true;

    // ✅ NUEVO: Fecha de creación
    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    public enum BusinessType {
        GYM,
        RETAIL,
        RESTAURANT,
        BEAUTY_SALON,
        COWORKING,
        HEALTH,           // Clínicas, consultorios
        EDUCATION,        // Academias, cursos
        PROFESSIONAL,     // Contadores, abogados
        OTHER             // Otros
    }
}