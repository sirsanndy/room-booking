package com.meetingroom.booking.infrastructure.persistence;

import com.meetingroom.booking.domain.model.Holiday;
import com.meetingroom.booking.domain.port.out.HolidayRepository;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDate;

@Component
public class HolidayRepositoryAdapter implements HolidayRepository {
    
    private final R2dbcHolidayRepository r2dbcHolidayRepository;
    
    public HolidayRepositoryAdapter(R2dbcHolidayRepository r2dbcHolidayRepository) {
        this.r2dbcHolidayRepository = r2dbcHolidayRepository;
    }
    
    @Override
    public Mono<Holiday> save(Holiday holiday) {
        return r2dbcHolidayRepository.save(holiday);
    }
    
    @Override
    public Mono<Holiday> findById(Long id) {
        return r2dbcHolidayRepository.findById(id);
    }
    
    @Override
    public Flux<Holiday> findAll() {
        return r2dbcHolidayRepository.findAll();
    }
    
    @Override
    public Flux<Holiday> findByDateBetween(LocalDate startDate, LocalDate endDate) {
        return r2dbcHolidayRepository.findByDateBetween(startDate, endDate);
    }
    
    @Override
    public Mono<Boolean> existsByDate(LocalDate date) {
        return r2dbcHolidayRepository.existsByDate(date);
    }
}
