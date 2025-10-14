package com.example.core.controller;

import com.example.core.context.TenantContext;
import com.example.core.dto.TenantDTO;
import com.example.core.model.Tenant;
import com.example.core.repository.TenantRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/config")
public class ConfigController {

    private final TenantRepository tenantRepository;

    public ConfigController(TenantRepository tenantRepository) {
        this.tenantRepository = tenantRepository;
    }

    @GetMapping("/current")
    public ResponseEntity<TenantDTO> getCurrentTenantConfig() {
        // Obtener tenant del contexto (inyectado por el interceptor)
        String tenantId = TenantContext.getCurrentTenant();

        if (tenantId == null) {
            // Fallback al tenant por defecto
            Tenant defaultTenant = tenantRepository.findBySubdomain("default")
                    .orElseThrow(() -> new RuntimeException("Default tenant not found"));
            return ResponseEntity.ok(toDTO(defaultTenant));
        }

        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new RuntimeException("Tenant not found"));

        return ResponseEntity.ok(toDTO(tenant));
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