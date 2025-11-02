package com.meetingroom.booking.domain.service;

import com.meetingroom.booking.domain.model.Booking;
import com.meetingroom.booking.domain.model.Holiday;
import com.meetingroom.booking.domain.port.in.DashboardUseCase;
import com.meetingroom.booking.domain.port.out.BookingRepository;
import com.meetingroom.booking.domain.port.out.HolidayRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Service
public class DashboardService implements DashboardUseCase {
    private static final Logger LOG = LoggerFactory.getLogger(DashboardService.class);
    
    private final BookingRepository bookingRepository;
    private final HolidayRepository holidayRepository;

    public DashboardService(BookingRepository bookingRepository, 
                           HolidayRepository holidayRepository) {
        this.bookingRepository = bookingRepository;
        this.holidayRepository = holidayRepository;
    }

    @Override
    public Flux<Booking> getUserBookings(Long userId) {
        LOG.info("Fetching all bookings for user: {}", userId);
        return bookingRepository.findBookingsByUser(userId);
    }

    @Override
    public Flux<Booking> getAllUpcomingBookings() {
        LOG.info("Fetching all upcoming bookings");
        LocalDateTime now = LocalDateTime.now();
        return bookingRepository.findAllUpcomingBookings(now);
    }

    @Override
    public Flux<Holiday> getUpcomingHolidays(LocalDate startDate, LocalDate endDate) {
        LOG.info("Fetching holidays between {} and {}", startDate, endDate);
        return holidayRepository.findByDateBetween(startDate, endDate);
    }

    @Override
    public Mono<Long> countTotalBookingsByUser(Long userId) {
        return bookingRepository.findBookingsByUser(userId)
            .filter(booking -> "CONFIRMED".equals(booking.getStatus()))
            .count();
    }

    @Override
    public Mono<Long> countUpcomingBookingsByUser(Long userId) {
        LocalDateTime now = LocalDateTime.now();
        return bookingRepository.findBookingsByUser(userId)
            .filter(booking -> "CONFIRMED".equals(booking.getStatus())
                && booking.getStartTime().isAfter(now))
            .count();
    }

    @Override
    public Mono<Long> countCompletedBookingsByUser(Long userId) {
        LocalDateTime now = LocalDateTime.now();
        return bookingRepository.findBookingsByUser(userId)
            .filter(booking -> "CONFIRMED".equals(booking.getStatus())
                && booking.getEndTime().isBefore(now))
            .count();
    }
}
