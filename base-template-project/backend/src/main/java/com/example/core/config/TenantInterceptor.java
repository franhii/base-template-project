package com.example.core.config;

import com.example.core.context.TenantContext;
import com.example.core.model.Tenant;
import com.example.core.repository.TenantRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class TenantInterceptor implements HandlerInterceptor {

    private final TenantRepository tenantRepository;

    public TenantInterceptor(TenantRepository tenantRepository) {
        this.tenantRepository = tenantRepository;
    }

    @Override
    public boolean preHandle(HttpServletRequest request,
                             HttpServletResponse response,
                             Object handler) {

        String subdomain = extractSubdomain(request);
        System.out.println("🔍 TenantInterceptor - Subdomain extraído: " + subdomain);

        if (subdomain == null || subdomain.equals("localhost")) {
            subdomain = request.getHeader("X-Tenant-Subdomain");
            System.out.println("🔍 TenantInterceptor - Header X-Tenant-Subdomain: " + subdomain);
        }

        if (subdomain == null) {
            subdomain = "default";
        }

        System.out.println("🔍 TenantInterceptor - Subdomain final: " + subdomain);

        Tenant tenant = tenantRepository.findBySubdomain(subdomain)
                .orElse(tenantRepository.findBySubdomain("default")
                        .orElseThrow(() -> new RuntimeException("No tenant found")));

        System.out.println("🏢 TenantInterceptor - Tenant encontrado: " + tenant.getBusinessName() + " (ID: " + tenant.getId() + ")");

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
        String host = request.getServerName(); // ej: "subdominio.tuapp.com"

        if (host.contains("localhost") || host.matches("\\d+\\.\\d+\\.\\d+\\.\\d+")) {
            return "default"; // En local usar tenant por defecto
        }

        String[] parts = host.split("\\.");
        if (parts.length > 2) {
            return parts[0]; // "subdominio"
        }

        return "default";
    }
}