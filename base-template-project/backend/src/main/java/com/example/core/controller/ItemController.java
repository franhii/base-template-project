package com.example.core.controller;

import com.example.core.dto.ProductDTO;
import com.example.core.dto.ServiceDTO;
import com.example.core.mapper.ItemMapper;
import com.example.core.model.Product;
import com.example.core.model.ServiceItem;
import com.example.core.repository.ProductRepository;
import com.example.core.repository.ServiceRepository;
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
    private final ItemMapper itemMapper; // ← Agregar

    public ItemController(ProductRepository productRepository,
                          ServiceRepository serviceRepository,
                          ItemMapper itemMapper) {
        this.productRepository = productRepository;
        this.serviceRepository = serviceRepository;
        this.itemMapper = itemMapper;
    }

    // ========== PRODUCTOS ==========

    //Obtenemos el producto por ID.
    @GetMapping("/products/{id}")
    public ResponseEntity<ProductDTO> getProduct(@PathVariable String id) {
        return productRepository.findById(id)
                .map(p -> ResponseEntity.ok(itemMapper.toProductDTO(p)))
                .orElse(ResponseEntity.notFound().build());
    }

    //Obtenemos todos los productos.
    @GetMapping("/products")
    public ResponseEntity<List<ProductDTO>> getAllProducts() {
        List<Product> products = productRepository.findByActiveTrue();
        List<ProductDTO> dtos = products.stream()
                .map(itemMapper::toProductDTO) // ← Usar mapper
                .collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }


    //Creamos un producto.
    @PostMapping("/products")
    @PreAuthorize("hasAnyRole('ADMIN', 'VENDEDOR')")
    public ResponseEntity<ProductDTO> createProduct(@RequestBody ProductDTO dto) {
        Product product = itemMapper.fromProductDTO(dto); // ← Usar mapper
        product = productRepository.save(product);
        return ResponseEntity.ok(itemMapper.toProductDTO(product)); // ← Usar mapper
    }

    // ========== SERVICIOS ==========

    @GetMapping("/services")
    //Obtenemos todos los servicios.
    public ResponseEntity<List<ServiceDTO>> getAllServices() {
        List<ServiceItem> services = serviceRepository.findByActiveTrue();
        List<ServiceDTO> dtos = services.stream()
                .map(itemMapper::toServiceDTO)
                .collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

    //Obtener servicio por id
    @GetMapping("/services/{id}")
    public ResponseEntity<ServiceDTO> getService(@PathVariable String id) {
        return serviceRepository.findById(id)
                .map(s -> ResponseEntity.ok(itemMapper.toServiceDTO(s)))
                .orElse(ResponseEntity.notFound().build());
    }

    //Creamos un servicio para ofrecer.
    @PostMapping("/services")
    @PreAuthorize("hasAnyRole('ADMIN', 'VENDEDOR')")
    public ResponseEntity<ServiceDTO> createService(@RequestBody ServiceDTO dto) {
        ServiceItem service = itemMapper.fromServiceDTO(dto);
        service = serviceRepository.save(service);
        return ResponseEntity.ok(itemMapper.toServiceDTO(service));
    }
}