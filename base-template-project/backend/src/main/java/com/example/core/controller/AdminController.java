package com.example.core.controller;

import com.example.core.dto.UserDTO;
import com.example.core.model.Role;
import com.example.core.model.User;
import com.example.core.repository.UserRepository;
import com.example.core.mapper.UserMapper;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')") // Solo admins
public class AdminController {

    private final UserRepository userRepository;
    private final UserMapper userMapper;

    public AdminController(UserRepository userRepository, UserMapper userMapper) {
        this.userRepository = userRepository;
        this.userMapper = userMapper;
    }

    //Obtiene TODOS LOS USUARIOS REGISTRADOS.
    @GetMapping("/users")
    public ResponseEntity<List<UserDTO>> getAllUsers() {
        List<User> users = userRepository.findAll();
        List<UserDTO> dtos = users.stream()
                .map(userMapper::toDto)
                .collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

    //Otorga ROL al user que por IDUser.
    @PatchMapping("/users/{id}/role")
    public ResponseEntity<?> updateUserRole(
            @PathVariable String id,
            @RequestBody Map<String, String> body) {

        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));

        String roleStr = body.get("role");
        try {
            Role newRole = Role.valueOf(roleStr);
            user.setRole(newRole);
            userRepository.save(user);
            return ResponseEntity.ok(userMapper.toDto(user));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Invalid role: " + roleStr));
        }
    }

    //Retorna la lista de usuario del rol(ADM,Vendedor,Cliente)
    @GetMapping("/users/role/{role}")
    public ResponseEntity<List<UserDTO>> getUsersByRole(@PathVariable String role) {
        try {
            Role roleEnum = Role.valueOf(role);
            List<User> users = userRepository.findByRole(roleEnum);
            List<UserDTO> dtos = users.stream()
                    .map(userMapper::toDto)
                    .collect(Collectors.toList());
            return ResponseEntity.ok(dtos);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }
}