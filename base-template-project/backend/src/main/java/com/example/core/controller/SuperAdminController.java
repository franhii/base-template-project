package com.example.core.controller;

import com.example.core.dto.ServiceDTO;
import com.example.core.dto.TenantDTO;
import com.example.core.model.ServiceItem;
import com.example.core.model.Tenant;
import com.example.core.model.TenantConfig;
import com.example.core.repository.ServiceRepository;
import com.example.core.repository.TenantRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Super Admin Controller - Solo accesible para SUPER_ADMIN
 * Permite gestionar TODOS los tenants de la plataforma
 */
@RestController
@RequestMapping("/api/super-admin")
@PreAuthorize("hasRole('SUPER_ADMIN')")
public class SuperAdminController {

    private final TenantRepository tenantRepository;
    private final ServiceRepository serviceRepository;

    public SuperAdminController(TenantRepository tenantRepository,
                                ServiceRepository serviceRepository) {
        this.tenantRepository = tenantRepository;
        this.serviceRepository = serviceRepository;
    }

    /**
     * GET /api/super-admin/tenants
     * Obtener TODOS los tenants (sin filtro por contexto)
     */
    @GetMapping("/tenants")
    public ResponseEntity<List<TenantDTO>> getAllTenants() {
        List<Tenant> tenants = tenantRepository.findAll();

        List<TenantDTO> dtos = tenants.stream()
                .map(this::toDTO)
                .collect(Collectors.toList());

        return ResponseEntity.ok(dtos);
    }

    /**
     * GET /api/super-admin/tenants/{id}
     * Obtener un tenant específico
     */
    @GetMapping("/tenants/{id}")
    public ResponseEntity<TenantDTO> getTenant(@PathVariable String id) {
        return tenantRepository.findById(id)
                .map(t -> ResponseEntity.ok(toDTO(t)))
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * POST /api/super-admin/tenants
     * Crear un nuevo tenant (onboarding de cliente)
     */
    @PostMapping("/tenants")
    public ResponseEntity<?> createTenant(@RequestBody TenantDTO dto) {
        // Validar que no exista el subdomain
        if (tenantRepository.findBySubdomain(dto.getSubdomain()).isPresent()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "El subdomain ya está en uso"));
        }

        Tenant tenant = new Tenant();
        tenant.setSubdomain(dto.getSubdomain());
        tenant.setBusinessName(dto.getBusinessName());
        tenant.setType(Tenant.BusinessType.valueOf(dto.getType()));
        tenant.setActive(true); // Por defecto activo

        // Config inicial por defecto
        TenantConfig config = dto.getConfig() != null ? dto.getConfig() : createDefaultConfig();
        tenant.setConfig(config);

        tenant = tenantRepository.save(tenant);

        return ResponseEntity.ok(toDTO(tenant));
    }

    /**
     * ✅ NUEVO: POST /api/super-admin/services
     * Crear servicio con booking (solo super admin)
     */
    @PostMapping("/services")
    public ResponseEntity<?> createService(@RequestBody ServiceDTO dto) {
        try {
            // Validar tenant
            if (dto.getTenantId() == null) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Debe especificar un tenant"));
            }

            Tenant tenant = tenantRepository.findById(dto.getTenantId())
                    .orElseThrow(() -> new RuntimeException("Tenant not found"));

            // Validar que el tenant tenga servicios habilitados
            if (tenant.getConfig() == null ||
                    tenant.getConfig().getFeatures() == null ||
                    !tenant.getConfig().getFeatures().isServices()) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Este tenant no tiene servicios habilitados"));
            }

            ServiceItem service = new ServiceItem();
            service.setName(dto.getName());
            service.setDescription(dto.getDescription());
            service.setPrice(dto.getPrice());
            service.setCategory(dto.getCategory());
            service.setImageUrl(dto.getImageUrl());
            service.setDurationMinutes(dto.getDurationMinutes());
            service.setScheduleType(ServiceItem.ScheduleType.valueOf(dto.getScheduleType()));
            service.setMaxCapacity(dto.getMaxCapacity());
            service.setRequiresBooking(dto.isRequiresBooking());
            service.setTenant(tenant);
            service.setActive(true);

            // Si requiere booking, configurar disponibilidad
            if (dto.isRequiresBooking()) {
                service.setAvailableDays(dto.getAvailableDays());
                service.setWorkStartTime(dto.getWorkStartTime());
                service.setWorkEndTime(dto.getWorkEndTime());
                service.setSlotIntervalMinutes(dto.getSlotIntervalMinutes());
            }

            service = serviceRepository.save(service);

            return ResponseEntity.ok(service);
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * PUT /api/super-admin/tenants/{id}
     * Actualizar información de un tenant
     */
    @PutMapping("/tenants/{id}")
    public ResponseEntity<?> updateTenant(
            @PathVariable String id,
            @RequestBody TenantDTO dto) {

        return tenantRepository.findById(id)
                .map(tenant -> {
                    tenant.setBusinessName(dto.getBusinessName());
                    tenant.setType(Tenant.BusinessType.valueOf(dto.getType()));

                    // Actualizar config si viene
                    if (dto.getConfig() != null) {
                        tenant.setConfig(dto.getConfig());
                    }

                    tenantRepository.save(tenant);
                    return ResponseEntity.ok(toDTO(tenant));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * PATCH /api/super-admin/tenants/{id}/toggle-status
     * Activar/Desactivar un tenant (suspender por falta de pago)
     */
    @PatchMapping("/tenants/{id}/toggle-status")
    public ResponseEntity<?> toggleTenantStatus(@PathVariable String id) {
        return tenantRepository.findById(id)
                .map(tenant -> {
                    tenant.setActive(!tenant.isActive());
                    tenantRepository.save(tenant);

                    String message = tenant.isActive()
                            ? "Tenant activado exitosamente"
                            : "Tenant suspendido exitosamente";

                    return ResponseEntity.ok(Map.of(
                            "message", message,
                            "tenant", toDTO(tenant)
                    ));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * PATCH /api/super-admin/tenants/{id}/features
     * Actualizar features habilitadas de un tenant
     */
    @PatchMapping("/tenants/{id}/features")
    public ResponseEntity<?> updateTenantFeatures(
            @PathVariable String id,
            @RequestBody TenantConfig.Features features) {

        return tenantRepository.findById(id)
                .map(tenant -> {
                    TenantConfig config = tenant.getConfig();
                    if (config == null) {
                        config = createDefaultConfig();
                    }
                    config.setFeatures(features);
                    tenant.setConfig(config);

                    tenantRepository.save(tenant);
                    return ResponseEntity.ok(toDTO(tenant));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * DELETE /api/super-admin/tenants/{id}
     * Eliminar un tenant (usar con precaución)
     */
    @DeleteMapping("/tenants/{id}")
    public ResponseEntity<?> deleteTenant(@PathVariable String id) {
        return tenantRepository.findById(id)
                .map(tenant -> {
                    // Validar que no sea el tenant por defecto
                    if ("default".equals(tenant.getSubdomain())) {
                        return ResponseEntity.badRequest()
                                .body(Map.of("error", "No se puede eliminar el tenant por defecto"));
                    }

                    tenantRepository.delete(tenant);
                    return ResponseEntity.ok(Map.of("message", "Tenant eliminado exitosamente"));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    // ========== HELPERS ==========

    private TenantDTO toDTO(Tenant tenant) {
        TenantDTO dto = new TenantDTO();
        dto.setId(tenant.getId());
        dto.setSubdomain(tenant.getSubdomain());
        dto.setBusinessName(tenant.getBusinessName());
        dto.setType(tenant.getType().name());
        dto.setActive(tenant.isActive());
        dto.setConfig(tenant.getConfig());
        dto.setCreatedAt(tenant.getCreatedAt());
        return dto;
    }

    private TenantConfig createDefaultConfig() {
        TenantConfig config = new TenantConfig();
        config.setPrimaryColor("#667eea");
        config.setSecondaryColor("#764ba2");

        TenantConfig.Features features = new TenantConfig.Features();
        features.setProducts(true);
        features.setServices(true);
        features.setCart(true);
        features.setCheckout(true);
        features.setMercadoPago(true);
        config.setFeatures(features);

        return config;
    }
}