package com.meetingroom.booking.infrastructure.persistence;

import com.meetingroom.booking.domain.model.Holiday;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDate;

@Repository
public interface R2dbcHolidayRepository extends R2dbcRepository<Holiday, Long> {
    
    @Query("SELECT * FROM holidays WHERE date BETWEEN :startDate AND :endDate ORDER BY date")
    Flux<Holiday> findByDateBetween(LocalDate startDate, LocalDate endDate);
    
    Mono<Boolean> existsByDate(LocalDate date);
}
