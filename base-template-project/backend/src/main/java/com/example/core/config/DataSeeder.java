package com.example.core.config;

import com.example.core.model.Role;
import com.example.core.model.User;
import com.example.core.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class DataSeeder {

    @Bean
    CommandLineRunner initDatabase(UserRepository userRepository,
                                   PasswordEncoder passwordEncoder) {
        return args -> {
            // Crear admin si no existe
            if (userRepository.findByEmail("admin@admin.com").isEmpty()) {
                User admin = new User();
                admin.setName("Admin");
                admin.setEmail("admin@admin.com");
                admin.setPassword(passwordEncoder.encode("admin123"));
                admin.setRole(Role.ADMIN);
                userRepository.save(admin);
                System.out.println("Admin user created: admin@admin.com / admin123");
            }
        };
    }
}