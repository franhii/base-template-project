package com.example.core.controller;

import com.example.core.dto.TenantDTO;
import com.example.core.model.Tenant;
import com.example.core.model.TenantConfig;
import com.example.core.repository.TenantRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/tenants")
@PreAuthorize("hasRole('ADMIN')") // Solo super admin
public class TenantController {

    private final TenantRepository tenantRepository;

    public TenantController(TenantRepository tenantRepository) {
        this.tenantRepository = tenantRepository;
    }

    @GetMapping
    public ResponseEntity<List<TenantDTO>> getAllTenants() {
        List<Tenant> tenants = tenantRepository.findAll();
        List<TenantDTO> dtos = tenants.stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

    @GetMapping("/{id}")
    public ResponseEntity<TenantDTO> getTenant(@PathVariable String id) {
        return tenantRepository.findById(id)
                .map(t -> ResponseEntity.ok(toDTO(t)))
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<TenantDTO> createTenant(@RequestBody TenantDTO dto) {
        // Validar que no exista el subdomain
        if (tenantRepository.findBySubdomain(dto.getSubdomain()).isPresent()) {
            return ResponseEntity.badRequest().build();
        }

        Tenant tenant = new Tenant();
        tenant.setSubdomain(dto.getSubdomain());
        tenant.setBusinessName(dto.getBusinessName());
        tenant.setType(Tenant.BusinessType.valueOf(dto.getType()));
        tenant.setConfig(dto.getConfig());

        tenant = tenantRepository.save(tenant);
        return ResponseEntity.ok(toDTO(tenant));
    }

    @PutMapping("/{id}")
    public ResponseEntity<TenantDTO> updateTenant(
            @PathVariable String id,
            @RequestBody TenantDTO dto) {

        return tenantRepository.findById(id)
                .map(tenant -> {
                    tenant.setBusinessName(dto.getBusinessName());
                    tenant.setType(Tenant.BusinessType.valueOf(dto.getType()));
                    tenant.setConfig(dto.getConfig());
                    tenantRepository.save(tenant);
                    return ResponseEntity.ok(toDTO(tenant));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    private TenantDTO toDTO(Tenant tenant) {
        TenantDTO dto = new TenantDTO();
        dto.setId(tenant.getId());
        dto.setSubdomain(tenant.getSubdomain());
        dto.setBusinessName(tenant.getBusinessName());
        dto.setType(tenant.getType().name());
        dto.setConfig(tenant.getConfig());
        return dto;
    }
}