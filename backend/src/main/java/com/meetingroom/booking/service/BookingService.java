package com.meetingroom.booking.service;

import com.meetingroom.booking.config.RateLimiter;
import com.meetingroom.booking.dto.BookingRequest;
import com.meetingroom.booking.dto.BookingResponse;
import com.meetingroom.booking.entity.Booking;
import com.meetingroom.booking.entity.Booking.BookingStatus;
import com.meetingroom.booking.entity.MeetingRoom;
import com.meetingroom.booking.entity.User;
import com.meetingroom.booking.exception.RateLimitExceededException;
import com.meetingroom.booking.repository.BookingRepository;
import com.meetingroom.booking.repository.MeetingRoomRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class BookingService {
    private static final Logger LOG = LoggerFactory.getLogger(BookingService.class);
    
    private static final LocalTime EARLIEST_BOOKING_TIME = LocalTime.of(7, 0);
    private static final LocalTime LATEST_BOOKING_TIME = LocalTime.of(22, 0);
    private static final int MAX_BOOKING_HOURS_PER_DAY = 9;
    
    private final BookingRepository bookingRepository;
    private final MeetingRoomRepository meetingRoomRepository;
    private final UserService userService;
    private final HolidayService holidayService;
    private final RateLimiter rateLimiter;
    
    public BookingService(BookingRepository bookingRepository,
                         MeetingRoomRepository meetingRoomRepository,
                         UserService userService,
                         HolidayService holidayService,
                         RateLimiter rateLimiter) {
        this.bookingRepository = bookingRepository;
        this.meetingRoomRepository = meetingRoomRepository;
        this.userService = userService;
        this.holidayService = holidayService;
        this.rateLimiter = rateLimiter;
    }
    
    /**
     * Create a new booking with pessimistic locking to prevent race conditions.
     * This ensures only one user can book a room for overlapping time periods.
     */
    @Transactional(isolation = Isolation.SERIALIZABLE)
    @CacheEvict(value = {"upcomingBookings", "roomBookings", "userBookings", "dashboardData"}, allEntries = true)
    public BookingResponse createBooking(BookingRequest request, String username) {
        LOG.info("Creating booking - user: {}, room: {}, start: {}, end: {}", 
            username, request.getRoomId(), request.getStartTime(), request.getEndTime());
        
        // Check rate limit for creating bookings
        String rateLimitKey = username + ":create_booking";
        if (!rateLimiter.allowRequest(rateLimitKey)) {
            long retryAfter = rateLimiter.getTimeUntilRefill(rateLimitKey);
            LOG.warn("Rate limit exceeded for user {} when creating booking. Retry after {} seconds", username, retryAfter);
            throw new RateLimitExceededException(
                "Too many booking requests. You can make up to 10 booking requests per minute. Please try again in " + retryAfter + " seconds.",
                retryAfter
            );
        }
        
        // Validate booking times
        if (request.getEndTime().isBefore(request.getStartTime())) {
            LOG.warn("Invalid booking time: end time before start time for user {}", username);
            throw new IllegalArgumentException("End time must be after start time");
        }
        
        if (request.getStartTime().isBefore(LocalDateTime.now())) {
            LOG.warn("Invalid booking time: booking in the past for user {}", username);
            throw new IllegalArgumentException("Cannot book in the past");
        }
        
        // Validate time restrictions
        LOG.debug("Validating time restrictions for booking");
        validateBookingTimeRestrictions(request.getStartTime(), request.getEndTime());
        
        // Get user (from cache if available)
        User user = userService.findByUsername(username);
        
        // Validate daily booking hours limit (9 hours per day)
        validateDailyBookingHoursLimit(user.getId(), request.getStartTime(), request.getEndTime());
        
        // Validate user doesn't have overlapping bookings in other rooms
        validateUserNotDoubleBooked(user.getId(), request.getStartTime(), request.getEndTime());
        
        // Acquire pessimistic write lock on the meeting room
        LOG.debug("Acquiring pessimistic lock on room: {}", request.getRoomId());
        MeetingRoom room = meetingRoomRepository.findByIdWithLock(request.getRoomId())
                .orElseThrow(() -> {
                    LOG.error("Meeting room not found: {}", request.getRoomId());
                    return new RuntimeException("Meeting room not found");
                });
        
        if (!room.getAvailable()) {
            LOG.warn("Room {} is not available", request.getRoomId());
            throw new RuntimeException("Meeting room is not available");
        }
        
        // Check for overlapping bookings with pessimistic lock
        LOG.debug("Checking for overlapping bookings");
        List<Booking> overlappingBookings = bookingRepository.findOverlappingBookingsWithLock(
                request.getRoomId(),
                request.getStartTime(),
                request.getEndTime(),
                BookingStatus.CONFIRMED
        );
        
        if (!overlappingBookings.isEmpty()) {
            LOG.warn("Overlapping booking found for room {} - count: {}", 
                request.getRoomId(), overlappingBookings.size());
            throw new RuntimeException("Room is already booked for the selected time period");
        }
        
        // Create the booking
        Booking booking = new Booking();
        booking.setRoom(room);
        booking.setUser(user);
        booking.setStartTime(request.getStartTime());
        booking.setEndTime(request.getEndTime());
        booking.setTitle(request.getTitle());
        booking.setDescription(request.getDescription());
        booking.setStatus(BookingStatus.CONFIRMED);
        
        Booking savedBooking = bookingRepository.save(booking);
        LOG.info("Booking created successfully - id: {}, user: {}, room: {}", 
            savedBooking.getId(), username, request.getRoomId());
        
        return mapToResponse(savedBooking);
    }
    
    @Transactional(readOnly = true)
    @Cacheable(value = "roomBookings", key = "#roomId")
    public List<BookingResponse> getUpcomingBookingsByRoom(Long roomId) {
        LOG.info("Fetching upcoming bookings for room: {} (cache miss)", roomId);
        List<Booking> bookings = bookingRepository.findUpcomingBookingsByRoom(roomId, LocalDateTime.now());
        return bookings.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }
    
    @Transactional(readOnly = true)
    @Cacheable(value = "userBookings", key = "#username")
    public List<BookingResponse> getUserBookings(String username) {
        LOG.info("Fetching bookings for user: {} (cache miss)", username);
        User user = userService.findByUsername(username);
        
        List<Booking> bookings = bookingRepository.findBookingsByUser(user.getId());
        return bookings.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }
    
    @Transactional(readOnly = true)
    @Cacheable(value = "upcomingBookings")
    public List<BookingResponse> getAllUpcomingBookings() {
        LOG.info("Fetching all upcoming bookings (cache miss)");
        List<Booking> bookings = bookingRepository.findAllUpcomingBookings(LocalDateTime.now());
        return bookings.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }
    
    @Transactional
    @CacheEvict(value = {"upcomingBookings", "roomBookings", "userBookings", "dashboardData"}, allEntries = true)
    public void cancelBooking(Long bookingId, String username) {
        LOG.info("Cancelling booking {} for user: {}", bookingId, username);
        
        // Check rate limit for cancelling bookings
        String rateLimitKey = username + ":cancel_booking";
        if (!rateLimiter.allowRequest(rateLimitKey)) {
            long retryAfter = rateLimiter.getTimeUntilRefill(rateLimitKey);
            LOG.warn("Rate limit exceeded for user {} when cancelling booking. Retry after {} seconds", username, retryAfter);
            throw new RateLimitExceededException(
                "Too many cancellation requests. You can make up to 10 cancellation requests per minute. Please try again in " + retryAfter + " seconds.",
                retryAfter
            );
        }
        
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> {
                    LOG.error("Booking not found: {}", bookingId);
                    return new RuntimeException("Booking not found");
                });
        
        User user = userService.findByUsername(username);
        
        if (!booking.getUser().getId().equals(user.getId())) {
            LOG.warn("User {} attempted to cancel booking {} owned by another user", 
                username, bookingId);
            throw new RuntimeException("You can only cancel your own bookings");
        }
        
        if (booking.getStatus() == BookingStatus.CANCELLED) {
            LOG.warn("Attempted to cancel already cancelled booking: {}", bookingId);
            throw new RuntimeException("Booking is already cancelled");
        }
        
        booking.setStatus(BookingStatus.CANCELLED);
        bookingRepository.save(booking);
        LOG.info("Booking {} cancelled successfully", bookingId);
    }
    
    @Transactional(readOnly = true)
    public boolean isRoomAvailable(Long roomId, LocalDateTime startTime, LocalDateTime endTime) {
        List<Booking> overlappingBookings = bookingRepository.findOverlappingBookingsWithLock(
                roomId, startTime, endTime, BookingStatus.CONFIRMED
        );
        return overlappingBookings.isEmpty();
    }
    
    private BookingResponse mapToResponse(Booking booking) {
        BookingResponse response = new BookingResponse();
        response.setId(booking.getId());
        response.setRoomId(booking.getRoom().getId());
        response.setRoomName(booking.getRoom().getName());
        response.setUserId(booking.getUser().getId());
        response.setUsername(booking.getUser().getUsername());
        response.setStartTime(booking.getStartTime());
        response.setEndTime(booking.getEndTime());
        response.setTitle(booking.getTitle());
        response.setDescription(booking.getDescription());
        response.setStatus(booking.getStatus().name());
        response.setCreatedAt(booking.getCreatedAt());
        return response;
    }
    

    private void validateBookingTimeRestrictions(LocalDateTime startTime, LocalDateTime endTime) {
        LocalDate startDate = startTime.toLocalDate();
        LocalDate endDate = endTime.toLocalDate();
        
        LOG.debug("Validating time restrictions - start: {}, end: {}", startTime, endTime);
        
        if (!startDate.equals(endDate)) {
            LOG.warn("Multi-day booking attempted: {} to {}", startDate, endDate);
            throw new IllegalArgumentException("Bookings cannot span multiple days");
        }
        
        DayOfWeek dayOfWeek = startDate.getDayOfWeek();
        if (dayOfWeek == DayOfWeek.SATURDAY || dayOfWeek == DayOfWeek.SUNDAY) {
            LOG.warn("Weekend booking attempted on: {} ({})", startDate, dayOfWeek);
            throw new IllegalArgumentException("Bookings are not allowed on weekends");
        }
        
        if (holidayService.existsByDate(startDate)) {
            LOG.warn("Holiday booking attempted on: {}", startDate);
            throw new IllegalArgumentException("Bookings are not allowed on holidays");
        }
        
        LocalTime startTimeOnly = startTime.toLocalTime();
        LocalTime endTimeOnly = endTime.toLocalTime();
        
        if (startTimeOnly.isBefore(EARLIEST_BOOKING_TIME)) {
            LOG.warn("Booking start time too early: {} (before 7:00 AM)", startTimeOnly);
            throw new IllegalArgumentException("Bookings cannot start before 7:00 AM");
        }
        
        if (endTimeOnly.isAfter(LATEST_BOOKING_TIME)) {
            LOG.warn("Booking end time too late: {} (after 10:00 PM)", endTimeOnly);
            throw new IllegalArgumentException("Bookings cannot end after 10:00 PM");
        }
        
        if (startTimeOnly.isAfter(LATEST_BOOKING_TIME) || endTimeOnly.isBefore(EARLIEST_BOOKING_TIME)) {
            LOG.warn("Booking time outside business hours: {}", startTimeOnly);
            throw new IllegalArgumentException("Bookings must be between 7:00 AM and 10:00 PM");
        }
        
        LOG.debug("Time restrictions validation passed");
    }

    private void validateDailyBookingHoursLimit(Long userId, LocalDateTime startTime, LocalDateTime endTime) {
        LOG.debug("Validating daily booking hours limit for user: {}", userId);
        
        LocalDate bookingDate = startTime.toLocalDate();
        
        List<Booking> existingBookings = bookingRepository.findBookingsByUserAndDate(
            userId, bookingDate, BookingStatus.CONFIRMED
        );
        
        long totalBookedMinutes = 0;
        for (Booking booking : existingBookings) {
            long durationMinutes = java.time.Duration.between(
                booking.getStartTime(), 
                booking.getEndTime()
            ).toMinutes();
            totalBookedMinutes += durationMinutes;
        }
        
        // Calculate new booking duration
        long newBookingMinutes = java.time.Duration.between(startTime, endTime).toMinutes();
        
        // Check if total exceeds 9 hours (540 minutes)
        long totalMinutes = totalBookedMinutes + newBookingMinutes;
        long maxMinutes = MAX_BOOKING_HOURS_PER_DAY * 60;
        
        if (totalMinutes > maxMinutes) {
            double totalHours = totalMinutes / 60.0;
            LOG.warn("User {} exceeded daily booking limit: {} hours (max {} hours)", 
                userId, totalHours, MAX_BOOKING_HOURS_PER_DAY);
            throw new IllegalArgumentException(
                String.format("You have exceeded the daily booking limit of %d hours. Current total: %.1f hours", 
                    MAX_BOOKING_HOURS_PER_DAY, totalHours)
            );
        }
        
        LOG.debug("Daily booking hours validation passed. Total: {} minutes of {} allowed", 
            totalMinutes, maxMinutes);
    }
    
    /**
     * Validates that the user doesn't have another booking during this time period
     */
    private void validateUserNotDoubleBooked(Long userId, LocalDateTime startTime, LocalDateTime endTime) {
        LOG.debug("Validating no double booking for user: {} during {} to {}", userId, startTime, endTime);
        
        List<Booking> overlappingBookings = bookingRepository.findOverlappingBookingsByUser(
            userId, startTime, endTime, BookingStatus.CONFIRMED
        );
        
        if (!overlappingBookings.isEmpty()) {
            Booking existingBooking = overlappingBookings.get(0);
            LOG.warn("User {} already has a booking (ID: {}) in room {} during this time period", 
                userId, existingBooking.getId(), existingBooking.getRoom().getName());
            throw new IllegalArgumentException(
                String.format("You already have a booking in '%s' from %s to %s during this time period",
                    existingBooking.getRoom().getName(),
                    existingBooking.getStartTime().toLocalTime(),
                    existingBooking.getEndTime().toLocalTime())
            );
        }
        
        LOG.debug("No double booking found - validation passed");
    }
}
