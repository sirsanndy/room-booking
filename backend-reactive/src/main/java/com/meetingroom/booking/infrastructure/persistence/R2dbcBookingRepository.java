package com.meetingroom.booking.infrastructure.persistence;

import com.meetingroom.booking.domain.model.Booking;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;

import java.time.LocalDateTime;

@Repository
public interface R2dbcBookingRepository extends R2dbcRepository<Booking, Long> {
    
    @Query("SELECT * FROM bookings WHERE room_id = :roomId AND start_time >= :currentTime AND status = :status ORDER BY start_time")
    Flux<Booking> findUpcomingBookingsByRoom(Long roomId, LocalDateTime currentTime, String status);
    
    @Query("SELECT * FROM bookings WHERE user_id = :userId ORDER BY start_time DESC")
    Flux<Booking> findBookingsByUser(Long userId);
    
    @Query("SELECT * FROM bookings WHERE start_time >= :currentTime AND status = :status ORDER BY start_time")
    Flux<Booking> findAllUpcomingBookings(LocalDateTime currentTime, String status);
    
    @Query("SELECT * FROM bookings WHERE room_id = :roomId AND status = :status " +
           "AND NOT (end_time <= :startTime OR start_time >= :endTime)")
    Flux<Booking> findOverlappingBookings(Long roomId, LocalDateTime startTime, 
                                         LocalDateTime endTime, String status);
    
    @Query("SELECT * FROM bookings WHERE user_id = :userId " +
           "AND start_time >= :startTime AND end_time <= :endTime")
    Flux<Booking> findUserBookingsBetween(Long userId, LocalDateTime startTime, LocalDateTime endTime);
}
