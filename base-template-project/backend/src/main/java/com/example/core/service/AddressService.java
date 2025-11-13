package com.example.core.service;

import com.example.core.context.TenantContext;
import com.example.core.dto.AddressRequest;
import com.example.core.dto.AddressResponse;
import com.example.core.exception.ResourceNotFoundException;
import com.example.core.model.Address;
import com.example.core.model.Tenant;
import com.example.core.model.User;
import com.example.core.repository.AddressRepository;
import com.example.core.repository.TenantRepository;
import com.example.core.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AddressService {

    private final AddressRepository addressRepository;
    private final UserRepository userRepository;
    private final TenantRepository tenantRepository;
    private final GeoRefService geoRefService;

    // ========== CREAR DIRECCIÓN ==========
    @Transactional
    public AddressResponse createAddress(String userId, AddressRequest request) {
        String tenantId = TenantContext.getCurrentTenant();

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));

        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Tenant no encontrado"));

        // Validar código postal argentino (4 dígitos)
        if (!request.getPostalCode().matches("\\d{4}")) {
            throw new IllegalArgumentException("Código postal inválido (debe ser 4 dígitos)");
        }

        // Validar provincia en GeoRef
        if (!geoRefService.isValidProvinceId(request.getProvinceId())) {
            throw new IllegalArgumentException("Provincia inválida");
        }

        // Si se marca como default, quitar default de las demás
        if (Boolean.TRUE.equals(request.getIsDefault())) {
            addressRepository.removeDefaultFromUserAddresses(userId, tenantId);
        }

        // Si es la primera dirección del usuario, marcarla como default automáticamente
        long addressCount = addressRepository.countByUserIdAndTenantId(userId, tenantId);
        boolean shouldBeDefault = addressCount == 0 || Boolean.TRUE.equals(request.getIsDefault());

        Address address = Address.builder()
                .user(user)
                .tenant(tenant)
                .street(request.getStreet())
                .streetNumber(request.getStreetNumber())
                .provinceId(request.getProvinceId())
                .provinceName(request.getProvinceName())
                .municipalityId(request.getMunicipalityId())
                .municipalityName(request.getMunicipalityName())
                .localityId(request.getLocalityId())
                .localityName(request.getLocalityName())
                .postalCode(request.getPostalCode())
                .apartment(request.getApartment())
                .reference(request.getReference())
                .latitude(request.getLatitude())
                .longitude(request.getLongitude())
                .isDefault(shouldBeDefault)
                .build();

        Address saved = addressRepository.save(address);
        log.info("Dirección creada: {} para user: {} en tenant: {}", saved.getId(), userId, tenantId);

        return mapToResponse(saved);
    }

    // ========== LISTAR DIRECCIONES DEL USUARIO ==========
    @Transactional(readOnly = true)
    public List<AddressResponse> getUserAddresses(String userId) {
        String tenantId = TenantContext.getCurrentTenant();

        List<Address> addresses = addressRepository
                .findByUserIdAndTenantIdOrderByIsDefaultDescCreatedAtDesc(userId, tenantId);

        return addresses.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    // ========== OBTENER DIRECCIÓN POR DEFECTO ==========
    @Transactional(readOnly = true)
    public AddressResponse getDefaultAddress(String userId) {
        String tenantId = TenantContext.getCurrentTenant();

        Address address = addressRepository
                .findByUserIdAndTenantIdAndIsDefaultTrue(userId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("No hay dirección por defecto"));

        return mapToResponse(address);
    }

    // ========== OBTENER DIRECCIÓN POR ID ==========
    @Transactional(readOnly = true)
    public AddressResponse getAddressById(String addressId, String userId) {
        String tenantId = TenantContext.getCurrentTenant();

        Address address = addressRepository
                .findByIdAndUserIdAndTenantId(addressId, userId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Dirección no encontrada"));

        return mapToResponse(address);
    }

    // ========== ACTUALIZAR DIRECCIÓN ==========
    @Transactional
    public AddressResponse updateAddress(String addressId, String userId, AddressRequest request) {
        String tenantId = TenantContext.getCurrentTenant();

        Address address = addressRepository
                .findByIdAndUserIdAndTenantId(addressId, userId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Dirección no encontrada"));

        // Validar código postal argentino (4 dígitos)
        if (!request.getPostalCode().matches("\\d{4}")) {
            throw new IllegalArgumentException("Código postal inválido (debe ser 4 dígitos)");
        }

        // Validar provincia en GeoRef
        if (!geoRefService.isValidProvinceId(request.getProvinceId())) {
            throw new IllegalArgumentException("Provincia inválida");
        }

        // Si se marca como default, quitar default de las demás
        if (Boolean.TRUE.equals(request.getIsDefault()) && !address.isDefault()) {
            addressRepository.removeDefaultFromUserAddresses(userId, tenantId);
        }

        address.setStreet(request.getStreet());
        address.setStreetNumber(request.getStreetNumber());
        address.setProvinceId(request.getProvinceId());
        address.setProvinceName(request.getProvinceName());
        address.setMunicipalityId(request.getMunicipalityId());
        address.setMunicipalityName(request.getMunicipalityName());
        address.setLocalityId(request.getLocalityId());
        address.setLocalityName(request.getLocalityName());
        address.setPostalCode(request.getPostalCode());
        address.setApartment(request.getApartment());
        address.setReference(request.getReference());
        address.setLatitude(request.getLatitude());
        address.setLongitude(request.getLongitude());
        address.setDefault(Boolean.TRUE.equals(request.getIsDefault()));

        Address updated = addressRepository.save(address);
        log.info("Dirección actualizada: {} para user: {}", addressId, userId);

        return mapToResponse(updated);
    }

    // ========== MARCAR COMO DIRECCIÓN POR DEFECTO ==========
    @Transactional
    public AddressResponse setAsDefault(String addressId, String userId) {
        String tenantId = TenantContext.getCurrentTenant();

        Address address = addressRepository
                .findByIdAndUserIdAndTenantId(addressId, userId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Dirección no encontrada"));

        if (!address.isDefault()) {
            addressRepository.removeDefaultFromUserAddresses(userId, tenantId);
            address.setDefault(true);
            addressRepository.save(address);
            log.info("Dirección {} marcada como default para user: {}", addressId, userId);
        }

        return mapToResponse(address);
    }

    // ========== ELIMINAR DIRECCIÓN ==========
    @Transactional
    public void deleteAddress(String addressId, String userId) {
        String tenantId = TenantContext.getCurrentTenant();

        Address address = addressRepository
                .findByIdAndUserIdAndTenantId(addressId, userId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Dirección no encontrada"));

        boolean wasDefault = address.isDefault();
        addressRepository.delete(address);
        log.info("Dirección eliminada: {} de user: {}", addressId, userId);

        // Si era la dirección por defecto, marcar otra como default
        if (wasDefault) {
            List<Address> remaining = addressRepository
                    .findByUserIdAndTenantIdOrderByIsDefaultDescCreatedAtDesc(userId, tenantId);

            if (!remaining.isEmpty()) {
                Address newDefault = remaining.get(0);
                newDefault.setDefault(true);
                addressRepository.save(newDefault);
                log.info("Nueva dirección por defecto: {}", newDefault.getId());
            }
        }
    }

    // ========== MAPPER ==========
    private AddressResponse mapToResponse(Address address) {
        return AddressResponse.builder()
                .id(address.getId())
                .street(address.getStreet())
                .streetNumber(address.getStreetNumber())
                .provinceId(address.getProvinceId())
                .provinceName(address.getProvinceName())
                .municipalityId(address.getMunicipalityId())
                .municipalityName(address.getMunicipalityName())
                .localityId(address.getLocalityId())
                .localityName(address.getLocalityName())
                .postalCode(address.getPostalCode())
                .apartment(address.getApartment())
                .reference(address.getReference())
                .latitude(address.getLatitude())
                .longitude(address.getLongitude())
                .isDefault(address.isDefault())
                .createdAt(address.getCreatedAt())
                .updatedAt(address.getUpdatedAt())
                .build();
    }
}