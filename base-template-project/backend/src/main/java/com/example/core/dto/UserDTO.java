package com.example.core.dto;

import com.example.core.model.Role;
import lombok.Data;

@Data
public class UserDTO {
    private String id;
    private String name;
    private String email;
    private String phone;
    private Role role;
    private String tenantId; // ← subdominio
    private String tenantName; // ← nombre del negocio
}