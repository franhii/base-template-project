package com.example.core.controller;

import com.example.core.dto.AuthRequest;
import com.example.core.dto.AuthResponse;
import com.example.core.dto.RegisterRequest;
import com.example.core.dto.UserDTO;
import com.example.core.mapper.UserMapper;
import com.example.core.model.Role;
import com.example.core.model.Tenant;
import com.example.core.model.User;
import com.example.core.repository.TenantRepository;
import com.example.core.repository.UserRepository;
import com.example.core.security.JwtUtil;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;
    private final UserMapper userMapper;
    private final TenantRepository tenantRepository;


    public AuthController(UserRepository userRepository,
                          PasswordEncoder passwordEncoder,
                          AuthenticationManager authenticationManager,
                          JwtUtil jwtUtil,
                          UserMapper userMapper,
                        TenantRepository tenantRepository) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtUtil = jwtUtil;
        this.userMapper = userMapper;
        this.tenantRepository = tenantRepository;

    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest req) {
        if (userRepository.findByEmail(req.getEmail()).isPresent()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Email already registered"));
        }

        // Obtener tenant por defecto (o el que corresponda)
        Tenant defaultTenant = tenantRepository.findBySubdomain("default")
                .orElseThrow(() -> new RuntimeException("Default tenant not found"));

        User user = userMapper.fromRegisterRequest(req);
        user.setPassword(passwordEncoder.encode(req.getPassword()));
        user.setRole(Role.CLIENTE);
        user.setTenant(defaultTenant); // ← IMPORTANTE: asignar tenant

        userRepository.save(user);

        return ResponseEntity.ok(userMapper.toDto(user));
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody AuthRequest authReq) {
        try {
            // Autenticar
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            authReq.getEmail(),
                            authReq.getPassword()
                    )
            );

            // Obtener usuario
            User user = userRepository.findByEmail(authReq.getEmail())
                    .orElseThrow(() -> new RuntimeException("User not found"));

            // Generar token con claims
            Map<String, Object> claims = new HashMap<>();
            claims.put("userId", user.getId());
            claims.put("role", user.getRole().name());

            String token = jwtUtil.generateToken(user.getEmail(), claims);

            return ResponseEntity.ok(new AuthResponse(token));

        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Invalid credentials"));
        }
    }

    @GetMapping("/me")
    public ResponseEntity<UserDTO> getCurrentUser(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        String email = authentication.getName();

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        return ResponseEntity.ok(userMapper.toDto(user));
    }
}