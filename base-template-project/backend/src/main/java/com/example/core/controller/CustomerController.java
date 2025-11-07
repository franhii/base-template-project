package com.example.core.controller;

import com.example.core.dto.OrderDTO;
import com.example.core.dto.UpdateProfileRequestDTO;
import com.example.core.dto.UserDTO;
import com.example.core.mapper.UserMapper;
import com.example.core.model.Booking;
import com.example.core.model.Order;
import com.example.core.model.User;
import com.example.core.repository.BookingRepository;
import com.example.core.repository.OrderRepository;
import com.example.core.repository.UserRepository;
import com.example.core.service.BookingService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Customer Controller - Panel del cliente
 * Permite ver órdenes, reservas y editar perfil
 */
@RestController
@RequestMapping("/api/customer")
@PreAuthorize("hasAnyRole('CLIENTE', 'VENDEDOR', 'ADMIN')")
public class CustomerController {

    private final UserRepository userRepository;
    private final OrderRepository orderRepository;
    private final BookingRepository bookingRepository;
    private final BookingService bookingService;
    private final UserMapper userMapper;

    public CustomerController(UserRepository userRepository,
                              OrderRepository orderRepository,
                              BookingRepository bookingRepository,
                              BookingService bookingService,
                              UserMapper userMapper) {
        this.userRepository = userRepository;
        this.orderRepository = orderRepository;
        this.bookingRepository = bookingRepository;
        this.bookingService = bookingService;
        this.userMapper = userMapper;
    }

    /**
     * GET /api/customer/profile
     * Obtener perfil del cliente actual
     */
    @GetMapping("/profile")
    public ResponseEntity<UserDTO> getProfile(Authentication authentication) {
        String email = authentication.getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        return ResponseEntity.ok(userMapper.toDto(user));
    }

    /**
     * PUT /api/customer/profile
     * Actualizar perfil del cliente
     */
    @PutMapping("/profile")
    public ResponseEntity<UserDTO> updateProfile(
            @RequestBody UpdateProfileRequestDTO request,
            Authentication authentication) {

        String email = authentication.getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Actualizar solo campos permitidos
        if (request.getName() != null && !request.getName().isBlank()) {
            user.setName(request.getName());
        }
        if (request.getPhone() != null) {
            user.setPhone(request.getPhone());
        }

        userRepository.save(user);
        return ResponseEntity.ok(userMapper.toDto(user));
    }

    /**
     * GET /api/customer/orders
     * Obtener todas mis órdenes
     */
    @GetMapping("/orders")
    public ResponseEntity<List<OrderDTO>> getMyOrders(Authentication authentication) {
        String email = authentication.getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        List<Order> orders = orderRepository.findByUserOrderByCreatedAtDesc(user);

        List<OrderDTO> dtos = orders.stream()
                .map(this::toOrderDTO)
                .collect(Collectors.toList());

        return ResponseEntity.ok(dtos);
    }

    /**
     * GET /api/customer/orders/{id}
     * Obtener detalle de una orden específica
     */
    @GetMapping("/orders/{id}")
    public ResponseEntity<OrderDTO> getOrder(
            @PathVariable String id,
            Authentication authentication) {

        String email = authentication.getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Order not found"));

        // Verificar que la orden pertenezca al usuario
        if (!order.getUser().getId().equals(user.getId())) {
            return ResponseEntity.status(403).build();
        }

        return ResponseEntity.ok(toOrderDTO(order));
    }

    /**
     * GET /api/customer/bookings
     * Obtener todas mis reservas
     */
    @GetMapping("/bookings")
    public ResponseEntity<List<Map<String, Object>>> getMyBookings(Authentication authentication) {
        String email = authentication.getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        List<Booking> bookings = bookingRepository.findByUserOrderByBookingDateDescStartTimeDesc(user);

        List<Map<String, Object>> dtos = bookings.stream()
                .map(this::toBookingDTO)
                .collect(Collectors.toList());

        return ResponseEntity.ok(dtos);
    }

    /**
     * GET /api/customer/bookings/upcoming
     * Obtener solo reservas próximas (futuras)
     */
    @GetMapping("/bookings/upcoming")
    public ResponseEntity<List<Map<String, Object>>> getUpcomingBookings(Authentication authentication) {
        String email = authentication.getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        LocalDate today = LocalDate.now();

        List<Booking> bookings = bookingRepository.findByUserOrderByBookingDateDescStartTimeDesc(user)
                .stream()
                .filter(b -> b.getBookingDate().isAfter(today) || b.getBookingDate().isEqual(today))
                .filter(b -> b.getStatus() == Booking.BookingStatus.CONFIRMED ||
                        b.getStatus() == Booking.BookingStatus.PENDING)
                .collect(Collectors.toList());

        List<Map<String, Object>> dtos = bookings.stream()
                .map(this::toBookingDTO)
                .collect(Collectors.toList());

        return ResponseEntity.ok(dtos);
    }

    /**
     * POST /api/customer/bookings/{id}/cancel
     * Cancelar una reserva (solo si falta más de 24 horas)
     */
    @PostMapping("/bookings/{id}/cancel")
    public ResponseEntity<?> cancelBooking(
            @PathVariable String id,
            @RequestBody(required = false) Map<String, String> body,
            Authentication authentication) {

        String email = authentication.getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Booking booking = bookingRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Booking not found"));

        // Verificar que la reserva pertenezca al usuario
        if (!booking.getUser().getId().equals(user.getId())) {
            return ResponseEntity.status(403).body(
                    Map.of("error", "No tienes permiso para cancelar esta reserva")
            );
        }

        // Verificar que no esté ya cancelada o completada
        if (booking.getStatus() == Booking.BookingStatus.CANCELLED ||
                booking.getStatus() == Booking.BookingStatus.COMPLETED) {
            return ResponseEntity.badRequest().body(
                    Map.of("error", "Esta reserva no se puede cancelar")
            );
        }

        // Verificar que falten más de 24 horas
        LocalDate today = LocalDate.now();
        if (booking.getBookingDate().isBefore(today.plusDays(1))) {
            return ResponseEntity.badRequest().body(
                    Map.of("error", "No se puede cancelar con menos de 24 horas de anticipación")
            );
        }

        String reason = body != null ? body.get("reason") : "Cancelado por el cliente";
        Booking cancelled = bookingService.cancelBooking(id, reason);

        return ResponseEntity.ok(toBookingDTO(cancelled));
    }

    /**
     * GET /api/customer/stats
     * Obtener estadísticas básicas del cliente
     */
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getCustomerStats(Authentication authentication) {
        String email = authentication.getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        List<Order> orders = orderRepository.findByUser(user);
        List<Booking> bookings = bookingRepository.findByUser(user);

        // Calcular estadísticas
        long totalOrders = orders.size();
        long completedOrders = orders.stream()
                .filter(o -> o.getStatus() == Order.OrderStatus.COMPLETED)
                .count();

        BigDecimal totalSpent = orders.stream()
                .filter(o -> o.getStatus() == Order.OrderStatus.CONFIRMED ||
                        o.getStatus() == Order.OrderStatus.COMPLETED)
                .map(Order::getTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        long totalBookings = bookings.size();
        long upcomingBookings = bookings.stream()
                .filter(b -> b.getBookingDate().isAfter(LocalDate.now()) ||
                        b.getBookingDate().isEqual(LocalDate.now()))
                .filter(b -> b.getStatus() == Booking.BookingStatus.CONFIRMED)
                .count();

        Map<String, Object> stats = Map.of(
                "totalOrders", totalOrders,
                "completedOrders", completedOrders,
                "totalSpent", totalSpent,
                "totalBookings", totalBookings,
                "upcomingBookings", upcomingBookings
        );

        return ResponseEntity.ok(stats);
    }

    // ========== HELPERS ==========

    private OrderDTO toOrderDTO(Order order) {
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
        return dto;
    }

    private Map<String, Object> toBookingDTO(Booking booking) {
        Map<String, Object> dto = new java.util.HashMap<>();
        dto.put("id", booking.getId());
        dto.put("serviceName", booking.getService().getName());
        dto.put("serviceId", booking.getService().getId());
        dto.put("bookingDate", booking.getBookingDate());
        dto.put("startTime", booking.getStartTime());
        dto.put("endTime", booking.getEndTime());
        dto.put("status", booking.getStatus().name());
        dto.put("notes", booking.getNotes());
        dto.put("createdAt", booking.getCreatedAt());

        // Info del servicio
        dto.put("servicePrice", booking.getService().getPrice());
        dto.put("serviceDuration", booking.getService().getDurationMinutes());
        dto.put("serviceImageUrl", booking.getService().getImageUrl());

        return dto;
    }
}