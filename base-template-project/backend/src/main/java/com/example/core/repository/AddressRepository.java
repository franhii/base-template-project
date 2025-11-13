package com.example.core.repository;

import com.example.core.model.Address;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AddressRepository extends JpaRepository<Address, String> {

    // Todas las direcciones de un usuario en su tenant
    List<Address> findByUserIdAndTenantIdOrderByIsDefaultDescCreatedAtDesc(String userId, String tenantId);

    // Dirección por defecto de un usuario en su tenant
    Optional<Address> findByUserIdAndTenantIdAndIsDefaultTrue(String userId, String tenantId);

    // Buscar dirección específica validando tenant y usuario
    Optional<Address> findByIdAndUserIdAndTenantId(String id, String userId, String tenantId);

    // Quitar el default de todas las direcciones de un usuario (para setear una nueva como default)
    @Modifying
    @Query("UPDATE Address a SET a.isDefault = false WHERE a.user.id = :userId AND a.tenant.id = :tenantId")
    void removeDefaultFromUserAddresses(@Param("userId") String userId, @Param("tenantId") String tenantId);

    // Contar direcciones de un usuario en su tenant
    long countByUserIdAndTenantId(String userId, String tenantId);
}