package com.meetingroom.booking.domain.port.out;

import com.meetingroom.booking.domain.model.Holiday;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDate;

/**
 * Output port for holiday persistence
 */
public interface HolidayRepository {
    
    Mono<Holiday> save(Holiday holiday);
    
    Mono<Holiday> findById(Long id);
    
    Flux<Holiday> findAll();
    
    Flux<Holiday> findByDateBetween(LocalDate startDate, LocalDate endDate);
    
    Mono<Boolean> existsByDate(LocalDate date);
}
