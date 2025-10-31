package com.example.core.repository;

import com.example.core.model.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Repository
public interface BookingRepository extends JpaRepository<Booking, String> {

    List<Booking> findByUser(User user);

    List<Booking> findByUserOrderByBookingDateDescStartTimeDesc(User user);

    List<Booking> findByTenant(Tenant tenant);

    List<Booking> findByService(ServiceItem service);

    List<Booking> findByOrder(Order order);

    List<Booking> findByServiceAndBookingDate(ServiceItem service, LocalDate bookingDate);

    List<Booking> findByServiceAndBookingDateBetween(ServiceItem service, LocalDate startDate, LocalDate endDate);

    List<Booking> findByStatus(Booking.BookingStatus status);

    @Query("SELECT b FROM Booking b WHERE b.service.id = :serviceId " +
           "AND b.bookingDate = :date " +
           "AND b.status IN ('PENDING', 'CONFIRMED') " +
           "AND ((b.startTime < :endTime AND b.endTime > :startTime))")
    List<Booking> findConflictingBookings(
            @Param("serviceId") String serviceId,
            @Param("date") LocalDate date,
            @Param("startTime") LocalTime startTime,
            @Param("endTime") LocalTime endTime
    );
}
