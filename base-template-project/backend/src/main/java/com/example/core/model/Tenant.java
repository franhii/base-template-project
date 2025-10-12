package com.example.core.model;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes; // Importa esta clase

@Entity
@Table(name = "tenants")
@Data
public class Tenant {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(unique = true)
    private String subdomain;

    private String businessName;

    @Enumerated(EnumType.STRING)
    private BusinessType type;

    @JdbcTypeCode(SqlTypes.JSON) // ¡Esta es la notación correcta!
    @Column(columnDefinition = "jsonb")
    private TenantConfig config;

    public enum BusinessType {
        GYM, RETAIL, RESTAURANT, BEAUTY_SALON, COWORKING
    }
}