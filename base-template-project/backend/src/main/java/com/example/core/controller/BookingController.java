package com.example.core.controller;

import com.example.core.model.Booking;
import com.example.core.service.BookingService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/bookings")
public class BookingController {

    private final BookingService bookingService;

    public BookingController(BookingService bookingService) {
        this.bookingService = bookingService;
    }

    /**
     * Obtener slots disponibles para un servicio en una fecha
     * GET /api/bookings/available?serviceId=xxx&date=2024-11-15
     */
    @GetMapping("/available")
    public ResponseEntity<List<Map<String, Object>>> getAvailableSlots(
            @RequestParam String serviceId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {

        List<Map<String, Object>> slots = bookingService.getAvailableSlots(serviceId, date);
        return ResponseEntity.ok(slots);
    }

    /**
     * Crear una reserva
     * POST /api/bookings
     * Body: { "serviceId": "xxx", "date": "2024-11-15", "startTime": "14:00", "notes": "..." }
     */
    @PostMapping
    @PreAuthorize("hasAnyRole('CLIENTE', 'VENDEDOR', 'ADMIN')")
    public ResponseEntity<Booking> createBooking(
            @RequestBody Map<String, String> request,
            Authentication authentication) {

        String serviceId = request.get("serviceId");
        LocalDate date = LocalDate.parse(request.get("date"));
        LocalTime startTime = LocalTime.parse(request.get("startTime"));
        String notes = request.get("notes");

        String userEmail = authentication.getName();

        Booking booking = bookingService.createBooking(serviceId, userEmail, date, startTime, notes);
        return ResponseEntity.ok(booking);
    }

    /**
     * Cancelar una reserva
     * POST /api/bookings/{id}/cancel
     */
    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasAnyRole('CLIENTE', 'VENDEDOR', 'ADMIN')")
    public ResponseEntity<Booking> cancelBooking(
            @PathVariable String id,
            @RequestBody(required = false) Map<String, String> body) {

        String reason = body != null ? body.get("reason") : "Cancelado por el usuario";
        Booking booking = bookingService.cancelBooking(id, reason);
        return ResponseEntity.ok(booking);
    }

    /**
     * Confirmar una reserva (interno, lo usa PaymentService)
     * POST /api/bookings/{id}/confirm
     */
    @PostMapping("/{id}/confirm")
    @PreAuthorize("hasAnyRole('VENDEDOR', 'ADMIN')")
    public ResponseEntity<Booking> confirmBooking(@PathVariable String id) {
        Booking booking = bookingService.confirmBooking(id);
        return ResponseEntity.ok(booking);
    }

    /**
     * Obtener mis reservas
     * GET /api/bookings/my-bookings
     */
    @GetMapping("/my-bookings")
    @PreAuthorize("hasAnyRole('CLIENTE', 'VENDEDOR', 'ADMIN')")
    public ResponseEntity<List<Booking>> getMyBookings(Authentication authentication) {
        String userEmail = authentication.getName();
        List<Booking> bookings = bookingService.getMyBookings(userEmail);
        return ResponseEntity.ok(bookings);
    }

    /**
     * Obtener reservas de un servicio en un rango de fechas
     * GET /api/bookings/service/{serviceId}?startDate=2024-11-01&endDate=2024-11-30
     */
    @GetMapping("/service/{serviceId}")
    @PreAuthorize("hasAnyRole('VENDEDOR', 'ADMIN')")
    public ResponseEntity<List<Booking>> getServiceBookings(
            @PathVariable String serviceId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        List<Booking> bookings = bookingService.getServiceBookings(serviceId, startDate, endDate);
        return ResponseEntity.ok(bookings);
    }
}
