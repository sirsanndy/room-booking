package com.meetingroom.booking.domain.port.in;

import com.meetingroom.booking.domain.model.Holiday;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDate;

/**
 * Input port for holiday use cases
 */
public interface HolidayUseCase {
    
    Mono<Holiday> createHoliday(Holiday holiday);
    
    Flux<Holiday> getAllHolidays();
    
    Flux<Holiday> getHolidaysBetween(LocalDate startDate, LocalDate endDate);
    
    Mono<Boolean> isHoliday(LocalDate date);
}
