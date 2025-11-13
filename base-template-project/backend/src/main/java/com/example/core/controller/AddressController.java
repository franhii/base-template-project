package com.example.core.controller;

import com.example.core.dto.AddressRequest;
import com.example.core.dto.AddressResponse;
import com.example.core.service.AddressService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/addresses")
@RequiredArgsConstructor
public class AddressController {

    private final AddressService addressService;

    // ========== CREAR DIRECCIÓN ==========
    @PostMapping
    public ResponseEntity<AddressResponse> createAddress(
            @Valid @RequestBody AddressRequest request,
            Authentication authentication) {

        String userId = authentication.getName(); // JWT subject = userId
        AddressResponse response = addressService.createAddress(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // ========== LISTAR MIS DIRECCIONES ==========
    @GetMapping
    public ResponseEntity<List<AddressResponse>> getMyAddresses(Authentication authentication) {
        String userId = authentication.getName();
        List<AddressResponse> addresses = addressService.getUserAddresses(userId);
        return ResponseEntity.ok(addresses);
    }

    // ========== OBTENER DIRECCIÓN POR DEFECTO ==========
    @GetMapping("/default")
    public ResponseEntity<AddressResponse> getDefaultAddress(Authentication authentication) {
        String userId = authentication.getName();
        AddressResponse address = addressService.getDefaultAddress(userId);
        return ResponseEntity.ok(address);
    }

    // ========== OBTENER DIRECCIÓN POR ID ==========
    @GetMapping("/{id}")
    public ResponseEntity<AddressResponse> getAddressById(
            @PathVariable String id,
            Authentication authentication) {

        String userId = authentication.getName();
        AddressResponse address = addressService.getAddressById(id, userId);
        return ResponseEntity.ok(address);
    }

    // ========== ACTUALIZAR DIRECCIÓN ==========
    @PutMapping("/{id}")
    public ResponseEntity<AddressResponse> updateAddress(
            @PathVariable String id,
            @Valid @RequestBody AddressRequest request,
            Authentication authentication) {

        String userId = authentication.getName();
        AddressResponse response = addressService.updateAddress(id, userId, request);
        return ResponseEntity.ok(response);
    }

    // ========== MARCAR COMO DIRECCIÓN POR DEFECTO ==========
    @PatchMapping("/{id}/set-default")
    public ResponseEntity<AddressResponse> setAsDefault(
            @PathVariable String id,
            Authentication authentication) {

        String userId = authentication.getName();
        AddressResponse response = addressService.setAsDefault(id, userId);
        return ResponseEntity.ok(response);
    }

    // ========== ELIMINAR DIRECCIÓN ==========
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteAddress(
            @PathVariable String id,
            Authentication authentication) {

        String userId = authentication.getName();
        addressService.deleteAddress(id, userId);
        return ResponseEntity.noContent().build();
    }
}
