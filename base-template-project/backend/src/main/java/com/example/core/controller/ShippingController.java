package com.example.core.controller;

import com.example.core.context.TenantContext;
import com.example.core.dto.ShippingCalculationRequest;
import com.example.core.dto.ShippingQuoteResponse;
import com.example.core.exception.ResourceNotFoundException;
import com.example.core.model.Address;
import com.example.core.model.Tenant;
import com.example.core.repository.AddressRepository;
import com.example.core.repository.TenantRepository;
import com.example.core.service.MercadoEnviosService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@RestController
@RequestMapping("/api/shipping")
@RequiredArgsConstructor
public class ShippingController {

    private final MercadoEnviosService mercadoEnviosService;
    private final AddressRepository addressRepository;
    private final TenantRepository tenantRepository;

    // ========== COTIZAR ENVÍO POR DIRECCIÓN ==========

    /**
     * POST /api/shipping/quote
     * Cotiza opciones de envío para una dirección del usuario
     *
     * Body:
     * {
     *   "addressId": "abc-123",
     *   "orderTotal": 15000.00,
     *   "dimensions": "20x20x10,1000" // opcional
     * }
     */
    @PostMapping("/quote")
    public ResponseEntity<ShippingQuoteResponse> quoteShipping(
            @Valid @RequestBody QuoteRequest request,
            Authentication authentication) {

        String userId = authentication.getName();
        String tenantId = TenantContext.getCurrentTenant();

        // Obtener tenant (origen)
        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Tenant no encontrado"));

        // Validar que el tenant tenga CP configurado
        String zipCodeFrom = getZipCodeFromTenant(tenant);
        if (zipCodeFrom == null) {
            throw new IllegalArgumentException("El negocio no tiene código postal configurado");
        }

        // Obtener dirección del usuario (destino)
        Address address = addressRepository.findByIdAndUserIdAndTenantId(
                        request.getAddressId(), userId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Dirección no encontrada"));

        // Dimensiones default si no se especifican
        String dimensions = request.getDimensions() != null
                ? request.getDimensions()
                : "20x20x10,1000"; // 20x20x10 cm, 1kg

        // Construir request para MercadoEnvíos
        ShippingCalculationRequest mlRequest = ShippingCalculationRequest.builder()
                .zipCodeFrom(zipCodeFrom)
                .zipCodeTo(address.getPostalCode())
                .dimensions(dimensions)
                .listCost(request.getOrderTotal())
                .freeShipping(false)
                .build();

        ShippingQuoteResponse quote = mercadoEnviosService.calculateShipping(mlRequest);

        return ResponseEntity.ok(quote);
    }

    // ========== VALIDAR DISPONIBILIDAD ==========

    /**
     * GET /api/shipping/available/{addressId}
     * Verifica si hay envío disponible para una dirección
     */
    @GetMapping("/available/{addressId}")
    public ResponseEntity<Boolean> isShippingAvailable(
            @PathVariable String addressId,
            Authentication authentication) {

        String userId = authentication.getName();
        String tenantId = TenantContext.getCurrentTenant();

        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Tenant no encontrado"));

        Address address = addressRepository.findByIdAndUserIdAndTenantId(
                        addressId, userId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Dirección no encontrada"));

        String zipCodeFrom = getZipCodeFromTenant(tenant);
        if (zipCodeFrom == null) {
            return ResponseEntity.ok(false);
        }

        boolean available = mercadoEnviosService.isShippingAvailable(
                zipCodeFrom,
                address.getPostalCode());

        return ResponseEntity.ok(available);
    }

    // ========== HELPER ==========

    private String getZipCodeFromTenant(Tenant tenant) {
        // Asumiendo que el tenant tiene un campo postalCode
        // Si está en TenantConfig, ajustar aquí
        try {
            // Opción 1: Si está directo en Tenant
            // return tenant.getPostalCode();

            // Opción 2: Si está en TenantConfig (JSONB)
            Object config = tenant.getConfig();
            if (config instanceof java.util.Map) {
                return (String) ((java.util.Map<?, ?>) config).get("postalCode");
            }
            return null;
        } catch (Exception e) {
            return null;
        }
    }

    // ========== DTO INTERNO ==========

    @lombok.Data
    public static class QuoteRequest {
        @jakarta.validation.constraints.NotBlank
        private String addressId;

        @jakarta.validation.constraints.NotNull
        private BigDecimal orderTotal;

        private String dimensions; // Opcional: "20x20x10,1000"
    }
}