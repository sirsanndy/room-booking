package com.meetingroom.booking.domain.port.in;

import com.meetingroom.booking.domain.model.Booking;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

/**
 * Input port for booking use cases
 */
public interface BookingUseCase {
    
    Mono<Booking> createBooking(Long roomId, Long userId, LocalDateTime startTime, 
                                LocalDateTime endTime, String title, String description, 
                                String username);
    
    Flux<Booking> getUpcomingBookingsByRoom(Long roomId);
    
    Flux<Booking> getUserBookings(Long userId);
    
    Flux<Booking> getAllUpcomingBookings();
    
    Mono<Void> cancelBooking(Long bookingId, Long userId);
}
