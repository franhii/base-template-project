package com.example.core.service;

import com.example.core.model.Booking;
import com.example.core.model.ServiceItem;
import com.example.core.model.User;
import com.example.core.repository.BookingRepository;
import com.example.core.repository.ServiceRepository;
import com.example.core.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class BookingService {

    private static final Logger logger = LoggerFactory.getLogger(BookingService.class);

    private final BookingRepository bookingRepository;
    private final ServiceRepository serviceRepository;
    private final UserRepository userRepository;

    public BookingService(BookingRepository bookingRepository,
                          ServiceRepository serviceRepository,
                          UserRepository userRepository) {
        this.bookingRepository = bookingRepository;
        this.serviceRepository = serviceRepository;
        this.userRepository = userRepository;
    }

    /**
     * Obtener slots disponibles para un servicio en una fecha específica
     */
    public List<Map<String, Object>> getAvailableSlots(String serviceId, LocalDate date) {
        logger.info("📅 Obteniendo slots disponibles para servicio: {} - Fecha: {}", serviceId, date);

        ServiceItem service = serviceRepository.findById(serviceId)
                .orElseThrow(() -> new RuntimeException("Service not found"));

        // Validar que el servicio requiera reserva
        if (!service.isRequiresBooking()) {
            throw new RuntimeException("Este servicio no requiere reserva");
        }

        // Validar que el día esté disponible
        if (!service.getAvailableDays().contains(date.getDayOfWeek())) {
            logger.warn("⚠️ El servicio no está disponible el día: {}", date.getDayOfWeek());
            return List.of();
        }

        // Generar todos los slots posibles del día
        List<Map<String, Object>> allSlots = generateAllSlots(service, date);

        // Obtener bookings existentes para ese día
        List<Booking> existingBookings = bookingRepository.findByServiceAndBookingDate(service, date);

        // Filtrar slots disponibles
        List<Map<String, Object>> availableSlots = new ArrayList<>();

        for (Map<String, Object> slot : allSlots) {
            LocalTime slotStart = (LocalTime) slot.get("startTime");
            LocalTime slotEnd = (LocalTime) slot.get("endTime");

            // Contar cuántos bookings activos hay en este slot
            long bookingsInSlot = existingBookings.stream()
                    .filter(b -> b.getStatus() == Booking.BookingStatus.PENDING ||
                                 b.getStatus() == Booking.BookingStatus.CONFIRMED)
                    .filter(b -> timesOverlap(b.getStartTime(), b.getEndTime(), slotStart, slotEnd))
                    .count();

            // Si hay capacidad disponible, agregar el slot
            int maxCapacity = service.getMaxCapacity() != null ? service.getMaxCapacity() : 1;
            if (bookingsInSlot < maxCapacity) {
                slot.put("availableSpots", maxCapacity - bookingsInSlot);
                slot.put("totalCapacity", maxCapacity);
                availableSlots.add(slot);
            }
        }

        logger.info("✅ Slots disponibles: {}/{}", availableSlots.size(), allSlots.size());
        return availableSlots;
    }

    /**
     * Generar todos los slots posibles para un servicio en un día
     */
    private List<Map<String, Object>> generateAllSlots(ServiceItem service, LocalDate date) {
        List<Map<String, Object>> slots = new ArrayList<>();

        LocalTime currentTime = service.getWorkStartTime();
        LocalTime endTime = service.getWorkEndTime();
        int intervalMinutes = service.getSlotIntervalMinutes() != null ? service.getSlotIntervalMinutes() : service.getDurationMinutes();

        while (currentTime.plusMinutes(service.getDurationMinutes()).isBefore(endTime) ||
               currentTime.plusMinutes(service.getDurationMinutes()).equals(endTime)) {

            Map<String, Object> slot = new HashMap<>();
            slot.put("startTime", currentTime);
            slot.put("endTime", currentTime.plusMinutes(service.getDurationMinutes()));
            slot.put("date", date);

            slots.add(slot);

            currentTime = currentTime.plusMinutes(intervalMinutes);
        }

        return slots;
    }

    /**
     * Verificar si dos rangos de tiempo se superponen
     */
    private boolean timesOverlap(LocalTime start1, LocalTime end1, LocalTime start2, LocalTime end2) {
        return start1.isBefore(end2) && end1.isAfter(start2);
    }

    /**
     * Crear una reserva
     */
    @Transactional
    public Booking createBooking(String serviceId, String userEmail, LocalDate date, LocalTime startTime, String notes) {
        logger.info("📝 Creando booking - Servicio: {} - Usuario: {} - Fecha: {} - Hora: {}",
                serviceId, userEmail, date, startTime);

        ServiceItem service = serviceRepository.findById(serviceId)
                .orElseThrow(() -> new RuntimeException("Service not found"));

        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Validar que el servicio requiera reserva
        if (!service.isRequiresBooking()) {
            throw new RuntimeException("Este servicio no requiere reserva");
        }

        // Validar que el día esté disponible
        if (!service.getAvailableDays().contains(date.getDayOfWeek())) {
            throw new RuntimeException("El servicio no está disponible ese día de la semana");
        }

        // Calcular hora de fin
        LocalTime endTime = startTime.plusMinutes(service.getDurationMinutes());

        // Validar que esté dentro del horario laboral
        if (startTime.isBefore(service.getWorkStartTime()) || endTime.isAfter(service.getWorkEndTime())) {
            throw new RuntimeException("El horario está fuera del horario de atención");
        }

        // Verificar que no haya conflictos
        List<Booking> conflicts = bookingRepository.findConflictingBookings(serviceId, date, startTime, endTime);

        int maxCapacity = service.getMaxCapacity() != null ? service.getMaxCapacity() : 1;
        if (conflicts.size() >= maxCapacity) {
            throw new RuntimeException("No hay capacidad disponible en ese horario");
        }

        // Crear booking
        Booking booking = new Booking();
        booking.setService(service);
        booking.setUser(user);
        booking.setTenant(user.getTenant());
        booking.setBookingDate(date);
        booking.setStartTime(startTime);
        booking.setEndTime(endTime);
        booking.setStatus(Booking.BookingStatus.PENDING);
        booking.setCustomerName(user.getName());
        booking.setCustomerEmail(user.getEmail());
        booking.setNotes(notes);

        Booking savedBooking = bookingRepository.save(booking);

        logger.info("✅ Booking creado: {}", savedBooking.getId());
        return savedBooking;
    }

    /**
     * Cancelar una reserva
     */
    @Transactional
    public Booking cancelBooking(String bookingId, String reason) {
        logger.info("🚫 Cancelando booking: {}", bookingId);

        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new RuntimeException("Booking not found"));

        booking.setStatus(Booking.BookingStatus.CANCELLED);
        if (reason != null && !reason.isEmpty()) {
            booking.setNotes(booking.getNotes() + " | CANCELACIÓN: " + reason);
        }

        Booking savedBooking = bookingRepository.save(booking);

        logger.info("✅ Booking cancelado");
        return savedBooking;
    }

    /**
     * Confirmar una reserva (cuando se confirma el pago)
     */
    @Transactional
    public Booking confirmBooking(String bookingId) {
        logger.info("✅ Confirmando booking: {}", bookingId);

        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new RuntimeException("Booking not found"));

        booking.setStatus(Booking.BookingStatus.CONFIRMED);

        Booking savedBooking = bookingRepository.save(booking);

        logger.info("✅ Booking confirmado");
        return savedBooking;
    }

    /**
     * Obtener reservas de un servicio en un rango de fechas
     */
    public List<Booking> getServiceBookings(String serviceId, LocalDate startDate, LocalDate endDate) {
        ServiceItem service = serviceRepository.findById(serviceId)
                .orElseThrow(() -> new RuntimeException("Service not found"));

        return bookingRepository.findByServiceAndBookingDateBetween(service, startDate, endDate);
    }

    /**
     * Obtener mis reservas
     */
    public List<Booking> getMyBookings(String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("User not found"));

        return bookingRepository.findByUserOrderByBookingDateDescStartTimeDesc(user);
    }
}
