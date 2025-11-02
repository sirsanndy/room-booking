package com.meetingroom.booking.domain.port.in;

import com.meetingroom.booking.domain.model.Booking;
import com.meetingroom.booking.domain.model.Holiday;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDate;

public interface DashboardUseCase {
    Flux<Booking> getUserBookings(Long userId);
    Flux<Booking> getAllUpcomingBookings();
    Flux<Holiday> getUpcomingHolidays(LocalDate startDate, LocalDate endDate);
    Mono<Long> countTotalBookingsByUser(Long userId);
    Mono<Long> countUpcomingBookingsByUser(Long userId);
    Mono<Long> countCompletedBookingsByUser(Long userId);
}
