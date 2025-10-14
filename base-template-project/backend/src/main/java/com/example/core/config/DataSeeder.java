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

                        // Features tipadas
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

                        return tenantRepository.save(t);
                    });

            // ... resto del código
        };
    }
}