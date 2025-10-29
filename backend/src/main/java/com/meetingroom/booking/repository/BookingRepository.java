package com.meetingroom.booking.repository;

import com.meetingroom.booking.entity.Booking;
import com.meetingroom.booking.entity.Booking.BookingStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface BookingRepository extends JpaRepository<Booking, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT b FROM Booking b WHERE b.room.id = :roomId " +
            "AND b.status = :status " +
            "AND ((b.startTime < :endTime AND b.endTime > :startTime))")
    List<Booking> findOverlappingBookingsWithLock(
            @Param("roomId") Long roomId,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime,
            @Param("status") BookingStatus status
    );

    // OPTIMIZED: JOIN FETCH to prevent N+1 queries (load room and user in single query)
    @Query("SELECT b FROM Booking b " +
            "JOIN FETCH b.room " +
            "JOIN FETCH b.user " +
            "WHERE b.room.id = :roomId " +
            "AND b.status = 'CONFIRMED' " +
            "AND b.startTime >= :startTime " +
            "ORDER BY b.startTime ASC")
    List<Booking> findUpcomingBookingsByRoom(
            @Param("roomId") Long roomId,
            @Param("startTime") LocalDateTime startTime
    );

    // OPTIMIZED: JOIN FETCH to prevent N+1 queries (load room and user in single query)
    @Query("SELECT b FROM Booking b " +
            "JOIN FETCH b.room " +
            "JOIN FETCH b.user " +
            "WHERE b.user.id = :userId " +
            "AND b.status = 'CONFIRMED' " +
            "ORDER BY b.startTime DESC")
    List<Booking> findBookingsByUser(@Param("userId") Long userId);

    // OPTIMIZED: JOIN FETCH to prevent N+1 queries (load room and user in single query)
    @Query("SELECT b FROM Booking b " +
            "JOIN FETCH b.room " +
            "JOIN FETCH b.user " +
            "WHERE b.status = 'CONFIRMED' " +
            "AND b.startTime >= :startTime " +
            "ORDER BY b.startTime ASC")
    List<Booking> findAllUpcomingBookings(@Param("startTime") LocalDateTime startTime);

    /**
     * Find all bookings for a user on a specific date with a given status
     */
    @Query("SELECT b FROM Booking b WHERE b.user.id = :userId " +
            "AND b.status = :status " +
            "AND FUNCTION('DATE', b.startTime) = FUNCTION('DATE', :date) " +
            "ORDER BY b.startTime ASC")
    List<Booking> findBookingsByUserAndDate(
            @Param("userId") Long userId,
            @Param("date") java.time.LocalDate date,
            @Param("status") BookingStatus status
    );

    /**
     * Find overlapping bookings for a user (to prevent double booking across rooms)
     */
    @Query("SELECT b FROM Booking b WHERE b.user.id = :userId " +
            "AND b.status = :status " +
            "AND ((b.startTime < :endTime AND b.endTime > :startTime))")
    List<Booking> findOverlappingBookingsByUser(
            @Param("userId") Long userId,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime,
            @Param("status") BookingStatus status
    );
}
