package com.example.core.controller;

import com.example.core.dto.ProductDTO;
import com.example.core.dto.ServiceDTO;
import com.example.core.mapper.ItemMapper;
import com.example.core.model.Product;
import com.example.core.model.ServiceItem;
import com.example.core.model.Tenant;
import com.example.core.repository.ProductRepository;
import com.example.core.repository.ServiceRepository;
import com.example.core.repository.TenantRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/items")
public class ItemController {

    private final ProductRepository productRepository;
    private final ServiceRepository serviceRepository;
    private final ItemMapper itemMapper;
    private final TenantRepository tenantRepository; // ← AGREGAR

    public ItemController(ProductRepository productRepository,
                          ServiceRepository serviceRepository,
                          ItemMapper itemMapper,
                          TenantRepository tenantRepository) { // ← AGREGAR
        this.productRepository = productRepository;
        this.serviceRepository = serviceRepository;
        this.itemMapper = itemMapper;
        this.tenantRepository = tenantRepository; // ← AGREGAR
    }

    // ========== PRODUCTOS ==========

    @GetMapping("/products/{id}")
    public ResponseEntity<ProductDTO> getProduct(@PathVariable String id) {
        return productRepository.findById(id)
                .map(p -> ResponseEntity.ok(itemMapper.toProductDTO(p)))
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/products")
    public ResponseEntity<List<ProductDTO>> getAllProducts() {
        List<Product> products = productRepository.findByActiveTrue();
        List<ProductDTO> dtos = products.stream()
                .map(itemMapper::toProductDTO)
                .collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

    @PostMapping("/products")
    @PreAuthorize("hasRole('ADMIN') or hasRole('VENDEDOR')") // ← CORREGIDO
    public ResponseEntity<ProductDTO> createProduct(@RequestBody ProductDTO dto) {
        // Obtener tenant por defecto
        Tenant defaultTenant = tenantRepository.findBySubdomain("default")
                .orElseThrow(() -> new RuntimeException("Default tenant not found"));

        Product product = itemMapper.fromProductDTO(dto);
        product.setTenant(defaultTenant); // ← IMPORTANTE: asignar tenant
        product = productRepository.save(product);
        return ResponseEntity.ok(itemMapper.toProductDTO(product));
    }

    // ========== SERVICIOS ==========

    @GetMapping("/services")
    public ResponseEntity<List<ServiceDTO>> getAllServices() {
        List<ServiceItem> services = serviceRepository.findByActiveTrue();
        List<ServiceDTO> dtos = services.stream()
                .map(itemMapper::toServiceDTO)
                .collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

    @GetMapping("/services/{id}")
    public ResponseEntity<ServiceDTO> getService(@PathVariable String id) {
        return serviceRepository.findById(id)
                .map(s -> ResponseEntity.ok(itemMapper.toServiceDTO(s)))
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/services")
    @PreAuthorize("hasRole('ADMIN') or hasRole('VENDEDOR')") // ← CORREGIDO
    public ResponseEntity<ServiceDTO> createService(@RequestBody ServiceDTO dto) {
        // Obtener tenant por defecto
        Tenant defaultTenant = tenantRepository.findBySubdomain("default")
                .orElseThrow(() -> new RuntimeException("Default tenant not found"));

        ServiceItem service = itemMapper.fromServiceDTO(dto);
        service.setTenant(defaultTenant); // ← IMPORTANTE: asignar tenant
        service = serviceRepository.save(service);
        return ResponseEntity.ok(itemMapper.toServiceDTO(service));
    }
}