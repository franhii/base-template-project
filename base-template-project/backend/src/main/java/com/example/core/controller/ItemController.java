package com.example.core.controller;

import com.example.core.context.TenantContext;
import com.example.core.dto.ProductDTO;
import com.example.core.dto.ServiceDTO;
import com.example.core.mapper.ItemMapper;
import com.example.core.model.Product;
import com.example.core.model.ServiceItem;
import com.example.core.model.Tenant;
import com.example.core.model.User;
import com.example.core.repository.ProductRepository;
import com.example.core.repository.ServiceRepository;
import com.example.core.repository.TenantRepository;
import com.example.core.repository.UserRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/items")
public class ItemController {

    private final ProductRepository productRepository;
    private final ServiceRepository serviceRepository;
    private final ItemMapper itemMapper;
    private final TenantRepository tenantRepository;
    private final UserRepository userRepository;

    public ItemController(ProductRepository productRepository,
                          ServiceRepository serviceRepository,
                          ItemMapper itemMapper,
                          TenantRepository tenantRepository,
                          UserRepository userRepository) {
        this.productRepository = productRepository;
        this.serviceRepository = serviceRepository;
        this.itemMapper = itemMapper;
        this.tenantRepository = tenantRepository;
        this.userRepository = userRepository;
    }

    // ========== PRODUCTOS ==========

    @GetMapping("/products/{id}")
    public ResponseEntity<ProductDTO> getProduct(@PathVariable String id) {
        return productRepository.findById(id)
                .map(p -> ResponseEntity.ok(itemMapper.toProductDTO(p)))
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/products")
    public ResponseEntity<List<ProductDTO>> getAllProducts(Authentication authentication) {
        if (authentication != null && authentication.isAuthenticated()) {
            String email = authentication.getName();
            User user = userRepository.findByEmail(email).orElse(null);

            if (user != null) {
                Tenant tenant = user.getTenant();
                List<Product> products = productRepository.findByActiveTrueAndTenant(tenant);
                List<ProductDTO> dtos = products.stream()
                        .map(itemMapper::toProductDTO)
                        .collect(Collectors.toList());
                return ResponseEntity.ok(dtos);
            }
        }

        List<Product> products = productRepository.findByActiveTrue();
        List<ProductDTO> dtos = products.stream()
                .map(itemMapper::toProductDTO)
                .collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

    @PostMapping("/products")
    @PreAuthorize("hasRole('ADMIN') or hasRole('VENDEDOR')")
    public ResponseEntity<ProductDTO> createProduct(
            @RequestBody ProductDTO dto,
            Authentication authentication) {

        String email = authentication.getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Tenant tenant = user.getTenant();

        Product product = itemMapper.fromProductDTO(dto);
        product.setTenant(tenant);
        product = productRepository.save(product);
        return ResponseEntity.ok(itemMapper.toProductDTO(product));
    }

    // ========== SERVICIOS ==========

    @GetMapping("/services")
    public ResponseEntity<List<ServiceDTO>> getAllServices() {
        String tenantId = TenantContext.getCurrentTenant();
        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new RuntimeException("Tenant not found"));

        List<ServiceItem> services = serviceRepository.findByActiveTrueAndTenant(tenant);
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
    @PreAuthorize("hasRole('ADMIN') or hasRole('VENDEDOR')")
    public ResponseEntity<ServiceDTO> createService(
            @RequestBody ServiceDTO dto,
            Authentication authentication) {

        String email = authentication.getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Tenant tenant = user.getTenant();

        // Validar que el tenant tenga servicios habilitados
        if (tenant.getConfig() == null ||
                tenant.getConfig().getFeatures() == null ||
                !tenant.getConfig().getFeatures().isServices()) {
            return ResponseEntity.badRequest()
                    .body(null); // O un DTO de error apropiado
        }

        ServiceItem service = itemMapper.fromServiceDTO(dto);
        service.setTenant(tenant);
        service.setActive(true);

        service = serviceRepository.save(service);
        return ResponseEntity.ok(itemMapper.toServiceDTO(service));
    }
}