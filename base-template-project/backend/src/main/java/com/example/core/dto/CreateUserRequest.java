package com.example.core.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CreateUserRequest {
    @NotBlank
    private String name;

    @Email
    @NotBlank
    private String email;

    private String phone;

    @NotBlank
    private String password;

    @NotBlank
    private String role; // "ADMIN", "VENDEDOR", "CLIENTE"

    @NotBlank
    private String tenantId; // ID del tenant al que pertenecerá
}