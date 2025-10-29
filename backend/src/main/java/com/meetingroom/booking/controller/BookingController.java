package com.meetingroom.booking.controller;

import com.meetingroom.booking.dto.BookingRequest;
import com.meetingroom.booking.dto.BookingResponse;
import com.meetingroom.booking.dto.MessageResponse;
import com.meetingroom.booking.service.BookingService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/bookings")
@CrossOrigin(origins = "*", maxAge = 3600)
public class BookingController {
    private static final Logger LOG = LoggerFactory.getLogger(BookingController.class);
    
    private final BookingService bookingService;

    public BookingController(BookingService bookingService) {
        this.bookingService = bookingService;
    }

    @PostMapping
    public ResponseEntity<?> createBooking(
            @Valid @RequestBody BookingRequest bookingRequest,
            Authentication authentication) {
        try {
            String username = authentication.getName();
            LOG.info("Creating booking for user: {}, room: {}, start: {}, end: {}", 
                username, bookingRequest.getRoomId(), 
                bookingRequest.getStartTime(), bookingRequest.getEndTime());
            BookingResponse booking = bookingService.createBooking(bookingRequest, username);
            LOG.info("Booking created successfully: id={}", booking.getId());
            return ResponseEntity.status(HttpStatus.CREATED).body(booking);
        } catch (IllegalArgumentException e) {
            LOG.warn("Booking validation failed for user {}: {}", authentication.getName(), e.getMessage());
            return ResponseEntity.badRequest().body(new MessageResponse(e.getMessage()));
        } catch (RuntimeException e) {
            LOG.error("Booking creation failed for user {}: {}", authentication.getName(), e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.CONFLICT).body(new MessageResponse(e.getMessage()));
        }
    }
    
    @GetMapping("/my-bookings")
    public ResponseEntity<List<BookingResponse>> getMyBookings(Authentication authentication) {
        String username = authentication.getName();
        LOG.info("Fetching bookings for user: {}", username);
        List<BookingResponse> bookings = bookingService.getUserBookings(username);
        LOG.info("Found {} bookings for user: {}", bookings.size(), username);
        return ResponseEntity.ok(bookings);
    }
    
    @GetMapping("/room/{roomId}")
    public ResponseEntity<List<BookingResponse>> getBookingsByRoom(@PathVariable Long roomId) {
        LOG.info("Fetching bookings for room: {}", roomId);
        List<BookingResponse> bookings = bookingService.getUpcomingBookingsByRoom(roomId);
        LOG.info("Found {} bookings for room: {}", bookings.size(), roomId);
        return ResponseEntity.ok(bookings);
    }
    
    @GetMapping
    public ResponseEntity<List<BookingResponse>> getAllUpcomingBookings() {
        LOG.info("Fetching all upcoming bookings");
        List<BookingResponse> bookings = bookingService.getAllUpcomingBookings();
        LOG.info("Found {} upcoming bookings", bookings.size());
        return ResponseEntity.ok(bookings);
    }
    
    @DeleteMapping("/{bookingId}")
    public ResponseEntity<?> cancelBooking(
            @PathVariable Long bookingId,
            Authentication authentication) {
        try {
            String username = authentication.getName();
            LOG.info("Cancelling booking {} for user: {}", bookingId, username);
            bookingService.cancelBooking(bookingId, username);
            LOG.info("Booking {} cancelled successfully", bookingId);
            return ResponseEntity.ok(new MessageResponse("Booking cancelled successfully"));
        } catch (RuntimeException e) {
            LOG.error("Failed to cancel booking {}: {}", bookingId, e.getMessage());
            return ResponseEntity.badRequest().body(new MessageResponse(e.getMessage()));
        }
    }
    
    @GetMapping("/check-availability")
    public ResponseEntity<?> checkAvailability(
            @RequestParam Long roomId,
            @RequestParam String startTime,
            @RequestParam String endTime) {
        try {
            LocalDateTime start = LocalDateTime.parse(startTime);
            LocalDateTime end = LocalDateTime.parse(endTime);
            boolean available = bookingService.isRoomAvailable(roomId, start, end);
            return ResponseEntity.ok(new MessageResponse(available ? "Room is available" : "Room is not available"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new MessageResponse("Invalid date format"));
        }
    }
}
