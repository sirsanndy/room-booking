package com.meetingroom.booking.repository;

import com.meetingroom.booking.entity.Holiday;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface HolidayRepository extends JpaRepository<Holiday, Long> {
    boolean existsByDate(LocalDate date);
    
    @Query("SELECT h FROM Holiday h WHERE h.date BETWEEN :startDate AND :endDate ORDER BY h.date")
    List<Holiday> findHolidaysBetween(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);
    
    List<Holiday> findByDateAfterOrderByDateAsc(LocalDate date);
}
