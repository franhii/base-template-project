package com.example.core.config;

import com.example.core.model.Role;
import com.example.core.model.Tenant;
import com.example.core.model.TenantConfig;
import com.example.core.model.User;
import com.example.core.repository.TenantRepository;
import com.example.core.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Configuration
public class DataSeeder {

    @Bean
    CommandLineRunner initDatabase(UserRepository userRepository,
                                   TenantRepository tenantRepository,
                                   PasswordEncoder passwordEncoder) {
        return args -> {
            // Crear tenant por defecto
            Tenant defaultTenant = tenantRepository.findBySubdomain("default")
                    .orElseGet(() -> {
                        Tenant t = new Tenant();
                        t.setSubdomain("default");
                        t.setBusinessName("Default Business");
                        t.setType(Tenant.BusinessType.RETAIL);

                        TenantConfig config = new TenantConfig();
                        config.setPrimaryColor("#3B82F6");
                        config.setSecondaryColor("#8B5CF6");
                        config.setCategories(List.of("General", "Electrónica", "Ropa"));

                        TenantConfig.Features features = new TenantConfig.Features();
                        features.setProducts(true);
                        features.setServices(true);
                        features.setCart(true);
                        features.setCheckout(true);
                        features.setMercadoPago(true);
                        features.setDelivery(false);
                        features.setBooking(false);

                        config.setFeatures(features);
                        t.setConfig(config);

                        System.out.println("✅ Default tenant created");
                        return tenantRepository.save(t);
                    });

            // Crear admin si no existe
            if (userRepository.findByEmail("admin@admin.com").isEmpty()) {
                User admin = new User();
                admin.setName("Admin");
                admin.setEmail("admin@admin.com");
                admin.setPassword(passwordEncoder.encode("admin123"));
                admin.setRole(Role.ADMIN);
                admin.setTenant(defaultTenant);
                userRepository.save(admin);
                System.out.println("✅ Admin user created: admin@admin.com / admin123");
            }
        };
    }
}