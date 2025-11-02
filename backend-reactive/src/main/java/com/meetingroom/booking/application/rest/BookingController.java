package com.meetingroom.booking.application.rest;

import com.meetingroom.booking.application.dto.BookingRequest;
import com.meetingroom.booking.application.dto.BookingResponse;
import com.meetingroom.booking.application.dto.MessageResponse;
import com.meetingroom.booking.domain.model.Booking;
import com.meetingroom.booking.domain.model.MeetingRoom;
import com.meetingroom.booking.domain.model.User;
import com.meetingroom.booking.domain.port.in.AuthUseCase;
import com.meetingroom.booking.domain.port.in.BookingUseCase;
import com.meetingroom.booking.domain.port.in.MeetingRoomUseCase;
import com.meetingroom.booking.domain.service.BookingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/bookings")
@CrossOrigin(origins = "*", maxAge = 3600)
@Tag(name = "Bookings", description = "Meeting room booking management endpoints (Reactive)")
@SecurityRequirement(name = "Bearer Authentication")
public class BookingController {
    private static final Logger LOG = LoggerFactory.getLogger(BookingController.class);
    
    private final BookingUseCase bookingUseCase;
    private final AuthUseCase authUseCase;
    private final MeetingRoomUseCase meetingRoomUseCase;
    
    public BookingController(BookingUseCase bookingUseCase,
                           AuthUseCase authUseCase,
                           MeetingRoomUseCase meetingRoomUseCase) {
        this.bookingUseCase = bookingUseCase;
        this.authUseCase = authUseCase;
        this.meetingRoomUseCase = meetingRoomUseCase;
    }
    
    @PostMapping
    @Operation(summary = "Create booking", description = "Create a new meeting room booking")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Booking created successfully",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = BookingResponse.class))),
        @ApiResponse(responseCode = "429", description = "Rate limit exceeded",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = MessageResponse.class))),
        @ApiResponse(responseCode = "400", description = "Invalid booking request",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = MessageResponse.class)))
    })
    public Mono<ResponseEntity<BookingResponse>> createBooking(@Valid @RequestBody BookingRequest request,
                                                 Authentication authentication) {
        String username = authentication.getName();
        LOG.info("Creating booking for user: {}", username);
        
        return authUseCase.getUserByUsername(username)
            .flatMap(user -> bookingUseCase.createBooking(
                request.getRoomId(),
                user.getId(),
                request.getStartTime(),
                request.getEndTime(),
                request.getTitle(),
                request.getDescription(),
                username
            ))
            .flatMap(this::mapToResponse)
            .map(ResponseEntity::ok)
            .onErrorResume(BookingService.RateLimitExceededException.class, error -> {
                LOG.warn("Rate limit exceeded: {}", error.getMessage());
                return Mono.just(ResponseEntity
                    .status(HttpStatus.TOO_MANY_REQUESTS)
                    .header("Retry-After", String.valueOf(error.getRetryAfter()))
                    .body(null));
            })
            .onErrorResume(error -> {
                LOG.error("Error creating booking: {}", error.getMessage());
                return Mono.just(ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(null));
            });
    }
    
    @GetMapping
    public Flux<BookingResponse> getAllUpcomingBookings() {
        LOG.info("Fetching all upcoming bookings");
        return bookingUseCase.getAllUpcomingBookings()
            .collectList()
            .flatMapMany(this::mapToResponseBatch);
    }
    
    @GetMapping("/room/{roomId}")
    public Flux<BookingResponse> getBookingsByRoom(@PathVariable Long roomId) {
        LOG.info("Fetching bookings for room: {}", roomId);
        return bookingUseCase.getUpcomingBookingsByRoom(roomId)
                .map(booking -> {
                    BookingResponse bookingResponse = new BookingResponse();
                    bookingResponse.setId(booking.getId());
                    bookingResponse.setRoomId(booking.getRoomId());
                    bookingResponse.setStartTime(booking.getStartTime());
                    bookingResponse.setEndTime(booking.getEndTime());
                    bookingResponse.setTitle(booking.getTitle());
                    bookingResponse.setDescription(booking.getDescription());
                    bookingResponse.setStatus(booking.getStatus());
                    return bookingResponse;
                });
    }
    
    @GetMapping("/my-bookings")
    public Flux<BookingResponse> getUserBookings(Authentication authentication) {
        String username = authentication.getName();
        LOG.info("Fetching bookings for user: {}", username);
        
        return authUseCase.getUserByUsername(username)
            .flatMapMany(user -> bookingUseCase.getUserBookings(user.getId()))
            .collectList()
            .flatMapMany(this::mapToResponseBatch);
    }
    
    @DeleteMapping("/{id}")
    public Mono<ResponseEntity<MessageResponse>> cancelBooking(@PathVariable Long id,
                                                 Authentication authentication) {
        String username = authentication.getName();
        LOG.info("Cancelling booking {} for user: {}", id, username);
        
        return authUseCase.getUserByUsername(username)
            .flatMap(user -> bookingUseCase.cancelBooking(id, user.getId()))
            .then(Mono.just(ResponseEntity.ok(new MessageResponse("Booking cancelled successfully"))))
            .onErrorResume(error -> {
                LOG.error("Error cancelling booking: {}", error.getMessage());
                return Mono.just(ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(new MessageResponse(error.getMessage())));
            });
    }
    
    /**
     * Batch mapping method to avoid N+1 query problem
     * Instead of fetching room and user for each booking individually (2N queries),
     * this method fetches all unique rooms and users in just 2 queries,
     * then maps them in memory.
     * 
     * Query count:
     * - Before: 1 (bookings) + N (rooms) + N (users) = 2N+1 queries
     * - After: 1 (bookings) + 1 (all rooms) + 1 (all users) = 3 queries
     */
    private Flux<BookingResponse> mapToResponseBatch(java.util.List<Booking> bookings) {
        if (bookings == null || bookings.isEmpty()) {
            LOG.debug("No bookings to map");
            return Flux.empty();
        }
        
        // Extract unique room IDs and user IDs
        Set<Long> roomIds = bookings.stream()
            .map(Booking::getRoomId)
            .collect(Collectors.toSet());
            
        Set<Long> userIds = bookings.stream()
            .map(Booking::getUserId)
            .collect(Collectors.toSet());
        
        LOG.info("Batch fetching {} unique rooms and {} unique users for {} bookings", 
            roomIds.size(), userIds.size(), bookings.size());
        
        // Fetch all rooms and users in parallel (2 queries instead of 2N)
        return Mono.zip(
            meetingRoomUseCase.getRoomsByIds(roomIds)
                .collectMap(MeetingRoom::getId),
            authUseCase.getUsersByIds(userIds)
                .collectMap(User::getId)
        ).flatMapMany(tuple -> {
            Map<Long, MeetingRoom> roomMap = tuple.getT1();
            Map<Long, User> userMap = tuple.getT2();
            
            LOG.debug("Fetched {} rooms and {} users from batch query", 
                roomMap.size(), userMap.size());
            
            // Map each booking using the in-memory maps (no additional queries)
            return Flux.fromIterable(bookings)
                .map(booking -> {
                    MeetingRoom room = roomMap.get(booking.getRoomId());
                    User user = userMap.get(booking.getUserId());
                    
                    BookingResponse response = new BookingResponse();
                    response.setId(booking.getId());
                    response.setRoomId(booking.getRoomId());
                    response.setUserId(booking.getUserId());
                    response.setStartTime(booking.getStartTime());
                    response.setEndTime(booking.getEndTime());
                    response.setTitle(booking.getTitle());
                    response.setDescription(booking.getDescription());
                    response.setStatus(booking.getStatus());
                    response.setCreatedAt(booking.getCreatedAt());
                    
                    // Set room and user details if found
                    if (room != null) {
                        response.setRoomName(room.getName());
                    }
                    if (user != null) {
                        response.setUsername(user.getUsername());
                    }
                    
                    return response;
                });
        });
    }
    
    /**
     * Single booking mapping method (used for createBooking where only 1 booking is returned)
     * For multiple bookings, use mapToResponseBatch instead to avoid N+1 queries
     */
    private Mono<BookingResponse> mapToResponse(Booking booking) {
        return Mono.zip(
            meetingRoomUseCase.getRoomById(booking.getRoomId()),
            authUseCase.getUserById(booking.getUserId())
        ).map(tuple -> {
            MeetingRoom room = tuple.getT1();
            User user = tuple.getT2();
            
            BookingResponse response = new BookingResponse();
            response.setId(booking.getId());
            response.setRoomId(booking.getRoomId());
            response.setRoomName(room.getName());
            response.setUserId(booking.getUserId());
            response.setUsername(user.getUsername());
            response.setStartTime(booking.getStartTime());
            response.setEndTime(booking.getEndTime());
            response.setTitle(booking.getTitle());
            response.setDescription(booking.getDescription());
            response.setStatus(booking.getStatus());
            response.setCreatedAt(booking.getCreatedAt());
            return response;
        }).onErrorResume(error -> {
            LOG.error("Error mapping booking to response: {}", error.getMessage());
            // Fallback mapping if room or user fetch fails
            BookingResponse response = new BookingResponse();
            response.setId(booking.getId());
            response.setRoomId(booking.getRoomId());
            response.setUserId(booking.getUserId());
            response.setStartTime(booking.getStartTime());
            response.setEndTime(booking.getEndTime());
            response.setTitle(booking.getTitle());
            response.setDescription(booking.getDescription());
            response.setStatus(booking.getStatus());
            response.setCreatedAt(booking.getCreatedAt());
            return Mono.just(response);
        });
    }
}
