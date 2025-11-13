package com.example.core.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.GenericGenerator;

import java.time.LocalDateTime;

@Entity
@Table(name = "addresses")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Address {

    @Id
    @GeneratedValue(generator = "UUID")
    @GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator")
    @Column(updatable = false, nullable = false)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;

    @Column(nullable = false)
    private String street; // Calle

    @Column(name = "street_number", nullable = false)
    private String streetNumber; // Número

    // Datos normalizados con GeoRef API
    @Column(name = "province_id", nullable = false)
    private String provinceId; // ID oficial provincia (ej: "82")

    @Column(name = "province_name", nullable = false)
    private String provinceName; // Nombre provincia (ej: "Santa Fe")

    @Column(name = "municipality_id")
    private String municipalityId; // ID oficial municipio

    @Column(name = "municipality_name", nullable = false)
    private String municipalityName; // Nombre municipio (ej: "Venado Tuerto")

    @Column(name = "locality_id")
    private String localityId; // ID oficial localidad

    @Column(name = "locality_name")
    private String localityName; // Nombre localidad

    @Column(name = "postal_code", nullable = false)
    private String postalCode; // Código postal (4 dígitos)

    private String apartment; // Depto/Piso (opcional)

    private String reference; // Referencias adicionales (ej: "Portón azul")

    // Coordenadas para cálculo de distancia (opcional)
    private Double latitude;
    private Double longitude;

    @Column(name = "is_default")
    private boolean isDefault; // Dirección por defecto del usuario

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}