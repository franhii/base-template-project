package com.example.core.model;

public enum Role {
    CLIENTE,      // Cliente regular que compra
    VENDEDOR,     // Vendedor del tenant (puede gestionar productos/servicios)
    ADMIN,        // Admin del tenant (acceso completo al tenant)
    SUPER_ADMIN   // ✅ NUEVO: Super admin de la plataforma (gestiona TODOS los tenants)
}