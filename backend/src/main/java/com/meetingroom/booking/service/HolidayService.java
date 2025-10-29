package com.meetingroom.booking.service;

import com.meetingroom.booking.entity.Holiday;
import com.meetingroom.booking.repository.HolidayRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
public class HolidayService {
    private static final Logger LOG = LoggerFactory.getLogger(HolidayService.class);
    private final HolidayRepository holidayRepository;

    public HolidayService(HolidayRepository holidayRepository) {
        this.holidayRepository = holidayRepository;
    }

    @Transactional(readOnly = true)
    @Cacheable(value = "holidays", key = "#startDate + '-' + #endDate")
    public List<Holiday> findHolidaysBetween(LocalDate startDate, LocalDate endDate) {
        LOG.info("Fetching holidays between {} and {} (cache miss)", startDate, endDate);
        return holidayRepository.findHolidaysBetween(startDate, endDate);
    }

    @Transactional(readOnly = true)
    @Cacheable(value = "upcomingHolidays", key = "#date")
    public List<Holiday> findUpcomingHolidays(LocalDate date) {
        LOG.info("Fetching upcoming holidays after {} (cache miss)", date);
        return holidayRepository.findByDateAfterOrderByDateAsc(date);
    }

    @Transactional(readOnly = true)
    public boolean existsByDate(LocalDate date) {
        return holidayRepository.existsByDate(date);
    }

    @Transactional
    @CacheEvict(value = {"holidays", "upcomingHolidays"}, allEntries = true)
    public void addHoliday(Holiday holiday) {
        holidayRepository.save(holiday);
    }

    @Transactional
    @CacheEvict(value = {"holidays", "upcomingHolidays"}, allEntries = true)
    public void updateHoliday(Holiday holiday) {
        holidayRepository.save(holiday);
    }

    @Transactional
    @CacheEvict(value = {"holidays", "upcomingHolidays"}, allEntries = true)
    public void deleteHoliday(Long id) {
        holidayRepository.deleteById(id);
    }
}
