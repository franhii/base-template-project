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
    private final UserRepository userRepository; // ← AGREGAR

    public ItemController(ProductRepository productRepository,
                          ServiceRepository serviceRepository,
                          ItemMapper itemMapper,
                          TenantRepository tenantRepository,
                          UserRepository userRepository) { // ← AGREGAR
        this.productRepository = productRepository;
        this.serviceRepository = serviceRepository;
        this.itemMapper = itemMapper;
        this.tenantRepository = tenantRepository;
        this.userRepository = userRepository; // ← AGREGAR
    }

    // ========== PRODUCTOS ==========

    @GetMapping("/products/{id}")
    public ResponseEntity<ProductDTO> getProduct(@PathVariable String id) {
        return productRepository.findById(id)
                .map(p -> ResponseEntity.ok(itemMapper.toProductDTO(p)))
                .orElse(ResponseEntity.notFound().build());
    }

    /* PARA PROD
    @GetMapping("/products")
    public ResponseEntity<List<ProductDTO>> getAllProducts() {
        // DEBUG
        String tenantId = TenantContext.getCurrentTenant();
        System.out.println("🔍 TenantContext.getCurrentTenant() = " + tenantId);

        if (tenantId == null) {
            System.out.println("⚠️ TenantContext es NULL, usando tenant por defecto");
            // Fallback temporal
            Tenant tenant = tenantRepository.findBySubdomain("default")
                    .orElseThrow(() -> new RuntimeException("Default tenant not found"));
            tenantId = tenant.getId();
        }

        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new RuntimeException("Tenant not found: "));

        System.out.println("🏢 Tenant encontrado: " + tenant.getBusinessName() + " (ID: " + tenant.getId() + ")");

        List<Product> products = productRepository.findByActiveTrueAndTenant(tenant);
        System.out.println("📦 Productos encontrados: " + products.size());

        List<ProductDTO> dtos = products.stream()
                .map(itemMapper::toProductDTO)
                .collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }
    */

    @GetMapping("/products")
    public ResponseEntity<List<ProductDTO>> getAllProducts(Authentication authentication) {
        // Si hay usuario autenticado, usar su tenant
        if (authentication != null && authentication.isAuthenticated()) {
            String email = authentication.getName();
            User user = userRepository.findByEmail(email).orElse(null);

            if (user != null) {
                Tenant tenant = user.getTenant();
                System.out.println("🏢 Usando tenant del usuario: " + tenant.getBusinessName() + "Tenant id: " + tenant.getId());

                List<Product> products = productRepository.findByActiveTrueAndTenant(tenant);
                List<ProductDTO> dtos = products.stream()
                        .map(itemMapper::toProductDTO)
                        .collect(Collectors.toList());
                return ResponseEntity.ok(dtos);
            }
        }

        // Fallback: traer de todos los tenants (solo para debug)
        System.out.println("⚠️ No hay usuario autenticado, trayendo todos los productos");
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

        // Obtener usuario autenticado y su tenant
        String email = authentication.getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Tenant tenant = user.getTenant();

        Product product = itemMapper.fromProductDTO(dto);
        product.setTenant(tenant); // ← Usar el tenant del usuario
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

        // Obtener usuario autenticado y su tenant
        String email = authentication.getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Tenant tenant = user.getTenant();

        ServiceItem service = itemMapper.fromServiceDTO(dto);
        service.setTenant(tenant); // ← Usar el tenant del usuario
        service = serviceRepository.save(service);
        return ResponseEntity.ok(itemMapper.toServiceDTO(service));
    }
}