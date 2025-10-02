package com.example.core.model;

import jakarta.persistence.*;
import lombok.Data;

import java.util.UUID;

@Table(name = "users")
@Data
@Entity
public class User {
    @Id
    private String id = UUID.randomUUID().toString();
    private String name;
    private String phone;
    @Column(unique = true, nullable = false)
    private String email;
    @Column(nullable = false)
    private String password;
    @Enumerated(EnumType.STRING)
    private Role role;
}
