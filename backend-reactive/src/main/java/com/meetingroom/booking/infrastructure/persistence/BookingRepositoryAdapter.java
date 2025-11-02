package com.meetingroom.booking.infrastructure.persistence;

import com.meetingroom.booking.domain.model.Booking;
import com.meetingroom.booking.domain.port.out.BookingRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.Objects;

@Component
public class BookingRepositoryAdapter implements BookingRepository {
    private static final Logger LOG = LoggerFactory.getLogger(BookingRepositoryAdapter.class);
    
    private final R2dbcBookingRepository r2dbcBookingRepository;
    
    public BookingRepositoryAdapter(R2dbcBookingRepository r2dbcBookingRepository) {
        this.r2dbcBookingRepository = r2dbcBookingRepository;
    }
    
    @Override
    public Mono<Booking> save(Booking booking) {
        return r2dbcBookingRepository.save(booking);
    }
    
    @Override
    public Mono<Booking> findById(Long id) {
        return r2dbcBookingRepository.findById(id);
    }
    
    @Override
    public Flux<Booking> findUpcomingBookingsByRoom(Long roomId, LocalDateTime currentTime) {
        String status = Booking.BookingStatus.CONFIRMED.name();
        LOG.debug("Finding upcoming bookings for room: {}, currentTime: {}, status: {}", roomId, currentTime, status);
        return r2dbcBookingRepository.findUpcomingBookingsByRoom(roomId, currentTime, status)
            .doOnNext(booking -> LOG.debug("Found booking: id={}, title={}, startTime={}", 
                booking.getId(), booking.getTitle(), booking.getStartTime()))
            .doOnComplete(() -> LOG.debug("Completed finding upcoming bookings for room: {}", roomId));
    }
    
    @Override
    public Flux<Booking> findBookingsByUser(Long userId) {
        LOG.debug("Finding bookings for user: {}", userId);
        return r2dbcBookingRepository.findBookingsByUser(userId);
    }
    
    @Override
    public Flux<Booking> findAllUpcomingBookings(LocalDateTime currentTime) {
        String status = Booking.BookingStatus.CONFIRMED.name();
        LOG.debug("Finding all upcoming bookings, currentTime: {}, status: {}", currentTime, status);
        return r2dbcBookingRepository.findAllUpcomingBookings(currentTime, status)
            .doOnNext(booking -> LOG.debug("Found booking: id={}, title={}, startTime={}", 
                booking.getId(), booking.getTitle(), booking.getStartTime()))
            .doOnComplete(() -> LOG.debug("Completed finding all upcoming bookings"));
    }
    
    @Override
    public Flux<Booking> findOverlappingBookings(Long roomId, LocalDateTime startTime, 
                                                 LocalDateTime endTime, String status) {
        return r2dbcBookingRepository.findOverlappingBookings(roomId, startTime, endTime, status);
    }
    
    @Override
    public Flux<Booking> findUserBookingsBetween(Long userId, LocalDateTime startTime, LocalDateTime endTime) {
        return r2dbcBookingRepository.findUserBookingsBetween(userId, startTime, endTime);
    }
    
    @Override
    public Mono<Void> deleteById(Long id) {
        return r2dbcBookingRepository.deleteById(id);
    }
}
