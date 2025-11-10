package com.example.core.config;

import com.example.core.context.TenantContext;
import com.example.core.model.Tenant;
import com.example.core.repository.TenantRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.io.IOException;

@Component
public class TenantInterceptor implements HandlerInterceptor {

    private final TenantRepository tenantRepository;

    public TenantInterceptor(TenantRepository tenantRepository) {
        this.tenantRepository = tenantRepository;
    }

    @Override
    public boolean preHandle(HttpServletRequest request,
                             HttpServletResponse response,
                             Object handler) throws IOException {

        // 🔥 NUEVO: Priorizar el header X-Tenant-Subdomain
        String subdomain = request.getHeader("X-Tenant-Subdomain");
        System.out.println("🔍 TenantInterceptor - Header X-Tenant-Subdomain: " + subdomain);

        // Si no hay header, extraer del dominio
        if (subdomain == null || subdomain.isEmpty()) {
            subdomain = extractSubdomain(request);
            System.out.println("🔍 TenantInterceptor - Subdomain del host: " + subdomain);
        }

        // Fallback a default
        if (subdomain == null || subdomain.isEmpty()) {
            subdomain = "default";
        }

        System.out.println("🔍 TenantInterceptor - Subdomain final: " + subdomain);

        Tenant tenant = tenantRepository.findBySubdomain(subdomain)
                .orElse(tenantRepository.findBySubdomain("default")
                        .orElseThrow(() -> new RuntimeException("No tenant found")));

        System.out.println("🏢 TenantInterceptor - Tenant encontrado: " + tenant.getBusinessName() + " (ID: " + tenant.getId() + ")");

        if (!tenant.isActive()) {
            System.out.println("⛔ Tenant suspendido: " + tenant.getBusinessName());
            response.setStatus(HttpStatus.FORBIDDEN.value());
            response.setContentType("application/json");
            response.getWriter().write(
                    "{\"error\": \"Tenant suspendido\", \"message\": \"Este negocio está temporalmente inactivo. Contacte al soporte.\"}"
            );
            return false;
        }

        TenantContext.setCurrentTenant(tenant.getId());
        return true;
    }



    @Override
    public void afterCompletion(HttpServletRequest request,
                                HttpServletResponse response,
                                Object handler,
                                Exception ex) {
        TenantContext.clear();
    }

    private String extractSubdomain(HttpServletRequest request) {
        String host = request.getServerName();

        if (host.contains("localhost") || host.matches("\\d+\\.\\d+\\.\\d+\\.\\d+")) {
            return null;
        }

        String[] parts = host.split("\\.");
        if (parts.length > 2) {
            return parts[0];
        }

        return "default";
    }
}