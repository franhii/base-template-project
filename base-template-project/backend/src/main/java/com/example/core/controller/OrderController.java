package com.example.core.controller;

import com.example.core.dto.CreateOrderRequest;
import com.example.core.dto.OrderDTO;
import com.example.core.dto.OrderItemDTO;
import com.example.core.model.Order;
import com.example.core.model.OrderItem;
import com.example.core.model.User;
import com.example.core.repository.OrderRepository;
import com.example.core.repository.UserRepository;
import com.example.core.service.OrderService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderService orderService;
    private final OrderRepository orderRepository;
    private final UserRepository userRepository;

    public OrderController(OrderService orderService,
                           OrderRepository orderRepository,
                           UserRepository userRepository) {
        this.orderService = orderService;
        this.orderRepository = orderRepository;
        this.userRepository = userRepository;
    }

    // Crear orden (checkout)
    @PostMapping
    @PreAuthorize("hasAnyRole('CLIENTE', 'VENDEDOR', 'ADMIN')")
    public ResponseEntity<OrderDTO> createOrder(
            @Valid @RequestBody CreateOrderRequest request,
            Authentication authentication) {

        String email = authentication.getName();
        Order order = orderService.createOrder(request, email);

        return ResponseEntity.ok(toDTO(order));
    }

    // Ver mis órdenes (cliente)
    @GetMapping("/my-orders")
    @PreAuthorize("hasAnyRole('CLIENTE', 'VENDEDOR', 'ADMIN')")
    public ResponseEntity<List<OrderDTO>> getMyOrders(Authentication authentication) {
        String email = authentication.getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        List<Order> orders = orderRepository.findByUserOrderByCreatedAtDesc(user);
        List<OrderDTO> dtos = orders.stream()
                .map(this::toDTO)
                .collect(Collectors.toList());

        return ResponseEntity.ok(dtos);
    }

    // Ver todas las órdenes del tenant (vendedor/admin)
    @GetMapping
    @PreAuthorize("hasAnyRole('VENDEDOR', 'ADMIN')")
    public ResponseEntity<List<OrderDTO>> getAllOrders(Authentication authentication) {
        String email = authentication.getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        List<Order> orders = orderRepository.findByTenantOrderByCreatedAtDesc(user.getTenant());
        List<OrderDTO> dtos = orders.stream()
                .map(this::toDTO)
                .collect(Collectors.toList());

        return ResponseEntity.ok(dtos);
    }

    // Ver detalle de una orden
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('CLIENTE', 'VENDEDOR', 'ADMIN')")
    public ResponseEntity<OrderDTO> getOrder(
            @PathVariable String id,
            Authentication authentication) {

        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Order not found"));

        // Verificar permisos: solo el dueño o vendedor/admin del tenant pueden ver
        String email = authentication.getName();
        User user = userRepository.findByEmail(email).orElseThrow();

        if (!order.getUser().getId().equals(user.getId()) &&
                !order.getTenant().getId().equals(user.getTenant().getId())) {
            return ResponseEntity.status(403).build();
        }

        return ResponseEntity.ok(toDTO(order));
    }

    // Cambiar estado de orden (solo vendedor/admin)
    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('VENDEDOR', 'ADMIN')")
    public ResponseEntity<OrderDTO> updateOrderStatus(
            @PathVariable String id,
            @RequestBody java.util.Map<String, String> body) {

        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Order not found"));

        String newStatus = body.get("status");
        order.setStatus(Order.OrderStatus.valueOf(newStatus));
        orderRepository.save(order);

        return ResponseEntity.ok(toDTO(order));
    }

    // Mapper manual
    private OrderDTO toDTO(Order order) {
        OrderDTO dto = new OrderDTO();
        dto.setId(order.getId());
        dto.setUserId(order.getUser().getId());
        dto.setUserName(order.getUser().getName());
        dto.setTenantId(order.getTenant().getId());
        dto.setTotal(order.getTotal());
        dto.setStatus(order.getStatus().name());
        dto.setPaymentMethod(order.getPaymentMethod() != null ? order.getPaymentMethod().name() : null);
        dto.setNotes(order.getNotes());
        dto.setCreatedAt(order.getCreatedAt());
        dto.setUpdatedAt(order.getUpdatedAt());

        List<OrderItemDTO> itemDtos = order.getItems().stream()
                .map(this::toItemDTO)
                .collect(Collectors.toList());
        dto.setItems(itemDtos);

        return dto;
    }

    private OrderItemDTO toItemDTO(OrderItem item) {
        OrderItemDTO dto = new OrderItemDTO();
        dto.setId(item.getId());
        dto.setItemId(item.getItem().getId());
        dto.setItemName(item.getItemName());
        dto.setItemType(item.getItemType());
        dto.setQuantity(item.getQuantity());
        dto.setPriceAtPurchase(item.getPriceAtPurchase());
        dto.setSubtotal(item.getPriceAtPurchase().multiply(BigDecimal.valueOf(item.getQuantity())));
        return dto;
    }
}