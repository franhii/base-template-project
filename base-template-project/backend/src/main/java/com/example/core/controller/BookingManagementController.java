package com.example.core.controller;

import com.example.core.model.Booking;
import com.example.core.model.ServiceItem;
import com.example.core.model.User;
import com.example.core.repository.BookingRepository;
import com.example.core.repository.ServiceRepository;
import com.example.core.repository.UserRepository;
import com.example.core.service.BookingService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Booking Management Controller - Para vendedores/admins de servicios
 * Gestionar reservas: ver calendario, modificar hora, cancelar, ver pagos
 */
@RestController
@RequestMapping("/api/booking-management")
@PreAuthorize("hasAnyRole('ADMIN', 'VENDEDOR')")
public class BookingManagementController {

    private final BookingRepository bookingRepository;
    private final ServiceRepository serviceRepository;
    private final UserRepository userRepository;
    private final BookingService bookingService;

    public BookingManagementController(BookingRepository bookingRepository,
                                       ServiceRepository serviceRepository,
                                       UserRepository userRepository,
                                       BookingService bookingService) {
        this.bookingRepository = bookingRepository;
        this.serviceRepository = serviceRepository;
        this.userRepository = userRepository;
        this.bookingService = bookingService;
    }

    /**
     * GET /api/booking-management/services
     * Obtener servicios del tenant con booking
     */
    @GetMapping("/services")
    public ResponseEntity<List<Map<String, Object>>> getServicesWithBooking(Authentication authentication) {
        User user = userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));

        List<ServiceItem> services = serviceRepository.findByActiveTrueAndTenant(user.getTenant())
                .stream()
                .filter(ServiceItem::isRequiresBooking)
                .collect(Collectors.toList());

        List<Map<String, Object>> result = services.stream()
                .map(s -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("id", s.getId());
                    map.put("name", s.getName());
                    map.put("durationMinutes", s.getDurationMinutes());
                    map.put("maxCapacity", s.getMaxCapacity());
                    return map;
                })
                .collect(Collectors.toList());

        return ResponseEntity.ok(result);
    }

    /**
     * GET /api/booking-management/calendar
     * Obtener calendario de reservas para un servicio en un rango de fechas
     */
    @GetMapping("/calendar")
    public ResponseEntity<List<Map<String, Object>>> getCalendar(
            @RequestParam String serviceId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            Authentication authentication) {

        User user = userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));

        ServiceItem service = serviceRepository.findById(serviceId)
                .orElseThrow(() -> new RuntimeException("Service not found"));

        // Verificar que el servicio pertenezca al tenant del usuario
        if (!service.getTenant().getId().equals(user.getTenant().getId())) {
            return ResponseEntity.status(403).build();
        }

        List<Booking> bookings = bookingRepository.findByServiceAndBookingDateBetween(
                service, startDate, endDate
        );

        List<Map<String, Object>> result = bookings.stream()
                .map(this::toBookingMap)
                .collect(Collectors.toList());

        return ResponseEntity.ok(result);
    }

    /**
     * GET /api/booking-management/bookings/today
     * Obtener reservas de hoy
     */
    @GetMapping("/bookings/today")
    public ResponseEntity<List<Map<String, Object>>> getTodayBookings(Authentication authentication) {
        User user = userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));

        LocalDate today = LocalDate.now();

        List<Booking> bookings = bookingRepository.findByTenant(user.getTenant())
                .stream()
                .filter(b -> b.getBookingDate().equals(today))
                .filter(b -> b.getStatus() != Booking.BookingStatus.CANCELLED)
                .collect(Collectors.toList());

        List<Map<String, Object>> result = bookings.stream()
                .map(this::toBookingMap)
                .collect(Collectors.toList());

        return ResponseEntity.ok(result);
    }

    /**
     * PUT /api/booking-management/bookings/{id}/reschedule
     * Modificar fecha/hora de una reserva
     */
    @PutMapping("/bookings/{id}/reschedule")
    public ResponseEntity<?> rescheduleBooking(
            @PathVariable String id,
            @RequestBody Map<String, String> body,
            Authentication authentication) {

        User user = userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));

        Booking booking = bookingRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Booking not found"));

        // Verificar que pertenezca al tenant
        if (!booking.getTenant().getId().equals(user.getTenant().getId())) {
            return ResponseEntity.status(403).build();
        }

        LocalDate newDate = LocalDate.parse(body.get("date"));
        LocalTime newTime = LocalTime.parse(body.get("time"));
        LocalTime endTime = newTime.plusMinutes(booking.getService().getDurationMinutes());

        // Verificar que no haya conflictos
        List<Booking> conflicts = bookingRepository.findConflictingBookings(
                booking.getService().getId(),
                newDate,
                newTime,
                endTime
        );

        // Excluir el booking actual de los conflictos
        conflicts = conflicts.stream()
                .filter(b -> !b.getId().equals(id))
                .collect(Collectors.toList());

        if (!conflicts.isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Ya hay una reserva en ese horario"));
        }

        booking.setBookingDate(newDate);
        booking.setStartTime(newTime);
        booking.setEndTime(endTime);
        bookingRepository.save(booking);

        return ResponseEntity.ok(toBookingMap(booking));
    }

    /**
     * POST /api/booking-management/bookings/{id}/cancel-admin
     * Cancelar reserva (admin/vendedor)
     */
    @PostMapping("/bookings/{id}/cancel-admin")
    public ResponseEntity<?> cancelBookingAdmin(
            @PathVariable String id,
            @RequestBody Map<String, String> body,
            Authentication authentication) {

        User user = userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));

        Booking booking = bookingRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Booking not found"));

        if (!booking.getTenant().getId().equals(user.getTenant().getId())) {
            return ResponseEntity.status(403).build();
        }

        String reason = body.getOrDefault("reason", "Cancelado por el vendedor");
        Booking cancelled = bookingService.cancelBooking(id, reason);

        return ResponseEntity.ok(toBookingMap(cancelled));
    }

    /**
     * GET /api/booking-management/bookings/{id}/payment-status
     * Ver estado de pago de una reserva
     */
    @GetMapping("/bookings/{id}/payment-status")
    public ResponseEntity<Map<String, Object>> getPaymentStatus(
            @PathVariable String id,
            Authentication authentication) {

        User user = userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));

        Booking booking = bookingRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Booking not found"));

        if (!booking.getTenant().getId().equals(user.getTenant().getId())) {
            return ResponseEntity.status(403).build();
        }

        Map<String, Object> paymentInfo = new HashMap<>();
        paymentInfo.put("bookingId", booking.getId());
        paymentInfo.put("bookingStatus", booking.getStatus().name());

        if (booking.getOrder() != null) {
            paymentInfo.put("orderId", booking.getOrder().getId());
            paymentInfo.put("orderStatus", booking.getOrder().getStatus().name());
            paymentInfo.put("total", booking.getOrder().getTotal());
            paymentInfo.put("paymentMethod", booking.getOrder().getPaymentMethod());
            paymentInfo.put("isPaid", booking.getStatus() == Booking.BookingStatus.CONFIRMED);
        } else {
            paymentInfo.put("isPaid", false);
            paymentInfo.put("message", "No hay orden asociada");
        }

        return ResponseEntity.ok(paymentInfo);
    }

    // ========== HELPER ==========

    private Map<String, Object> toBookingMap(Booking booking) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", booking.getId());
        map.put("serviceName", booking.getService().getName());
        map.put("serviceId", booking.getService().getId());
        map.put("customerName", booking.getCustomerName());
        map.put("customerEmail", booking.getCustomerEmail());
        map.put("customerPhone", booking.getCustomerPhone());
        map.put("bookingDate", booking.getBookingDate());
        map.put("startTime", booking.getStartTime());
        map.put("endTime", booking.getEndTime());
        map.put("status", booking.getStatus().name());
        map.put("notes", booking.getNotes());
        map.put("createdAt", booking.getCreatedAt());

        // Info de pago
        if (booking.getOrder() != null) {
            map.put("isPaid", booking.getStatus() == Booking.BookingStatus.CONFIRMED);
            map.put("orderTotal", booking.getOrder().getTotal());
        } else {
            map.put("isPaid", false);
        }

        return map;
    }
}