package com.meetingroom.booking.domain.service;

import com.meetingroom.booking.domain.model.Holiday;
import com.meetingroom.booking.domain.port.in.HolidayUseCase;
import com.meetingroom.booking.domain.port.out.CachePort;
import com.meetingroom.booking.domain.port.out.HolidayRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDate;

@Service
public class HolidayService implements HolidayUseCase {
    private static final Logger LOG = LoggerFactory.getLogger(HolidayService.class);
    private static final long HOLIDAY_CACHE_TTL = 86400000; // 24 hours
    
    private final HolidayRepository holidayRepository;
    private final CachePort cachePort;
    
    public HolidayService(HolidayRepository holidayRepository,
                         CachePort cachePort) {
        this.holidayRepository = holidayRepository;
        this.cachePort = cachePort;
    }
    
    @Override
    public Mono<Holiday> createHoliday(Holiday holiday) {
        LOG.info("Creating holiday: {} on {}", holiday.getName(), holiday.getDate());
        return holidayRepository.save(holiday)
            .flatMap(savedHoliday -> clearHolidayCaches().thenReturn(savedHoliday))
            .doOnSuccess(savedHoliday -> LOG.info("Holiday created successfully: {}", savedHoliday.getId()));
    }
    
    @Override
    public Flux<Holiday> getAllHolidays() {
        String cacheKey = "holidays";
        
        return cachePort.get(cacheKey, Holiday[].class)
            .flatMapMany(holidays -> Flux.fromArray(holidays))
            .switchIfEmpty(
                holidayRepository.findAll()
                    .collectList()
                    .flatMap(holidayList -> {
                        if (!holidayList.isEmpty()) {
                            Holiday[] holidaysArray = holidayList.toArray(new Holiday[0]);
                            return cachePort.set(cacheKey, holidaysArray, HOLIDAY_CACHE_TTL)
                                .thenReturn(holidaysArray);
                        }
                        return Mono.just(new Holiday[0]);
                    })
                    .flatMapMany(Flux::fromArray)
            )
            .doOnSubscribe(s -> LOG.info("Fetching all holidays"));
    }
    
    @Override
    public Flux<Holiday> getHolidaysBetween(LocalDate startDate, LocalDate endDate) {
        String cacheKey = "holidays:" + startDate + ":" + endDate;
        
        return cachePort.get(cacheKey, Holiday[].class)
            .flatMapMany(holidays -> Flux.fromArray(holidays))
            .switchIfEmpty(
                holidayRepository.findByDateBetween(startDate, endDate)
                    .collectList()
                    .flatMap(holidayList -> {
                        if (!holidayList.isEmpty()) {
                            Holiday[] holidaysArray = holidayList.toArray(new Holiday[0]);
                            return cachePort.set(cacheKey, holidaysArray, HOLIDAY_CACHE_TTL)
                                .thenReturn(holidaysArray);
                        }
                        return Mono.just(new Holiday[0]);
                    })
                    .flatMapMany(Flux::fromArray)
            )
            .doOnSubscribe(s -> LOG.info("Fetching holidays between {} and {}", startDate, endDate));
    }
    
    @Override
    public Mono<Boolean> isHoliday(LocalDate date) {
        String cacheKey = "holiday:check:" + date;
        
        return cachePort.get(cacheKey, Boolean.class)
            .switchIfEmpty(
                holidayRepository.existsByDate(date)
                    .flatMap(exists -> 
                        cachePort.set(cacheKey, exists, HOLIDAY_CACHE_TTL)
                            .thenReturn(exists)
                    )
            );
    }
    
    private Mono<Boolean> clearHolidayCaches() {
        return cachePort.delete("holidays");
    }
}
