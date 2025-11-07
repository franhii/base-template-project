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

import java.util.List;

@Configuration
public class DataSeeder {

    @Bean
    CommandLineRunner initDatabase(UserRepository userRepository,
                                   TenantRepository tenantRepository,
                                   PasswordEncoder passwordEncoder) {
        return args -> {
            // ========== CREAR TENANT POR DEFECTO ==========
            Tenant defaultTenant = tenantRepository.findBySubdomain("default")
                    .orElseGet(() -> {
                        Tenant t = new Tenant();
                        t.setSubdomain("default");
                        t.setBusinessName("Default Business");
                        t.setType(Tenant.BusinessType.RETAIL);
                        t.setActive(true); // ✅ NUEVO

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

            // ========== CREAR ADMIN DEL TENANT ==========
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

            // ========== ✅ CREAR SUPER ADMIN (NUEVO) ==========
            if (userRepository.findByEmail("superadmin@platform.com").isEmpty()) {
                User superAdmin = new User();
                superAdmin.setName("Super Admin");
                superAdmin.setEmail("superadmin@platform.com");
                superAdmin.setPassword(passwordEncoder.encode("superadmin123"));
                superAdmin.setRole(Role.SUPER_ADMIN);
                superAdmin.setTenant(defaultTenant); // Asociado al tenant default
                userRepository.save(superAdmin);
                System.out.println("✅ Super Admin created: superadmin@platform.com / superadmin123");
            }
        };
    }
}