package com.meetingroom.booking.domain.port.out;

import com.meetingroom.booking.domain.model.Booking;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

/**
 * Output port for booking persistence
 */
public interface BookingRepository {
    
    Mono<Booking> save(Booking booking);
    
    Mono<Booking> findById(Long id);
    
    Flux<Booking> findUpcomingBookingsByRoom(Long roomId, LocalDateTime currentTime);
    
    Flux<Booking> findBookingsByUser(Long userId);
    
    Flux<Booking> findAllUpcomingBookings(LocalDateTime currentTime);
    
    Flux<Booking> findOverlappingBookings(Long roomId, LocalDateTime startTime, 
                                         LocalDateTime endTime, String status);
    
    Flux<Booking> findUserBookingsBetween(Long userId, LocalDateTime startTime, LocalDateTime endTime);
    
    Mono<Void> deleteById(Long id);
}
