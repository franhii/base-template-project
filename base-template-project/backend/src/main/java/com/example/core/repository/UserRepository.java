package com.example.core.repository;

import com.example.core.model.Role;
import com.example.core.model.Tenant;
import com.example.core.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, String> {
    Optional<User> findByEmail(String email);
    List<User> findByRole(Role role);
    List<User> findByTenant(Tenant tenant); // ← AGREGAR
}