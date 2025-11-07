package com.example.core.dto;

import lombok.Data;

@Data
public class UpdateProfileRequestDTO {
    private String name;
    private String phone;
    // Email no se puede cambiar por seguridad
}