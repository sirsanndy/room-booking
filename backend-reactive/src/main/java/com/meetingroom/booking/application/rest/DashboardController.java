package com.meetingroom.booking.application.rest;

import com.meetingroom.booking.application.dto.BookingResponse;
import com.meetingroom.booking.application.dto.CalendarEventDTO;
import com.meetingroom.booking.application.dto.DashboardDTO;
import com.meetingroom.booking.domain.model.Booking;
import com.meetingroom.booking.domain.model.Holiday;
import com.meetingroom.booking.domain.port.in.AuthUseCase;
import com.meetingroom.booking.domain.port.in.BookingUseCase;
import com.meetingroom.booking.domain.port.in.DashboardUseCase;
import com.meetingroom.booking.domain.port.in.MeetingRoomUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/dashboard")
@CrossOrigin(origins = "*", maxAge = 3600)
@Tag(name = "Dashboard", description = "Dashboard data endpoints (Reactive)")
@SecurityRequirement(name = "Bearer Authentication")
public class DashboardController {
    private static final Logger LOG = LoggerFactory.getLogger(DashboardController.class);
    
    private final DashboardUseCase dashboardUseCase;
    private final AuthUseCase authUseCase;
    private final MeetingRoomUseCase meetingRoomUseCase;

    public DashboardController(DashboardUseCase dashboardUseCase,
                              AuthUseCase authUseCase,
                              MeetingRoomUseCase meetingRoomUseCase) {
        this.dashboardUseCase = dashboardUseCase;
        this.authUseCase = authUseCase;
        this.meetingRoomUseCase = meetingRoomUseCase;
    }

    @GetMapping
    @Operation(summary = "Get dashboard data", description = "Get dashboard data including calendar events and statistics")
    @ApiResponse(responseCode = "200", description = "Dashboard data retrieved successfully",
        content = @Content(mediaType = "application/json", schema = @Schema(implementation = DashboardDTO.class)))
    public Mono<ResponseEntity<DashboardDTO>> getDashboard(Authentication authentication) {
        String username = authentication.getName();
        LOG.info("Fetching dashboard data for user: {}", username);
        
        return authUseCase.getUserByUsername(username)
            .flatMap(user -> {
                LocalDate today = LocalDate.now();
                LocalDate threeMonthsLater = today.plusMonths(3);
                
                // Fetch all data in parallel
                Mono<List<CalendarEventDTO>> bookingEventsMono = createBookingEvents();
                Mono<List<CalendarEventDTO>> holidayEventsMono = createHolidayEvents(today, threeMonthsLater);
                Mono<DashboardDTO.DashboardStats> statsMono = calculateStats(user.getId());
                
                // Combine all results
                return Mono.zip(bookingEventsMono, holidayEventsMono, statsMono)
                    .map(tuple -> {
                        List<CalendarEventDTO> allEvents = new ArrayList<>();
                        allEvents.addAll(tuple.getT1());
                        allEvents.addAll(tuple.getT2());
                        
                        DashboardDTO dashboard = new DashboardDTO(allEvents, tuple.getT3());
                        LOG.info("Dashboard data prepared with {} events for user: {}", 
                            allEvents.size(), username);
                        return ResponseEntity.ok(dashboard);
                    });
            })
            .onErrorResume(error -> {
                LOG.error("Failed to fetch dashboard data for user: {}", username, error);
                return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build());
            });
    }
    
    private Mono<List<CalendarEventDTO>> createBookingEvents() {
        return dashboardUseCase.getAllUpcomingBookings()
            .flatMap(booking -> 
                meetingRoomUseCase.getRoomById(booking.getRoomId())
                    .map(room -> {
                        CalendarEventDTO event = new CalendarEventDTO();
                        event.setId(booking.getId());
                        event.setTitle(booking.getTitle());
                        event.setStart(booking.getStartTime());
                        event.setEnd(booking.getEndTime());
                        event.setDescription(booking.getDescription());
                        event.setRoomName(room.getName());
                        event.setRoomId(room.getId());
                        event.setStatus(booking.getStatus());
                        event.setType("booking");
                        
                        // Set color based on status
                        if ("CONFIRMED".equals(booking.getStatus())) {
                            event.setColor("#3498db"); // Blue
                        } else {
                            event.setColor("#95a5a6"); // Gray for cancelled
                        }
                        
                        return event;
                    })
            )
            .collectList();
    }
    
    private Mono<List<CalendarEventDTO>> createHolidayEvents(LocalDate startDate, LocalDate endDate) {
        return dashboardUseCase.getUpcomingHolidays(startDate, endDate)
            .map(holiday -> {
                CalendarEventDTO event = new CalendarEventDTO();
                event.setId(holiday.getId());
                event.setTitle(holiday.getName());
                event.setStart(holiday.getDate().atStartOfDay());
                event.setEnd(holiday.getDate().atTime(23, 59));
                event.setDescription(holiday.getDescription());
                event.setType("holiday");
                event.setColor("#e74c3c"); // Red for holidays
                return event;
            })
            .collectList();
    }
    
    private Mono<DashboardDTO.DashboardStats> calculateStats(Long userId) {
        Mono<Long> totalBookingsMono = dashboardUseCase.countTotalBookingsByUser(userId);
        Mono<Long> upcomingBookingsMono = dashboardUseCase.countUpcomingBookingsByUser(userId);
        Mono<Long> completedBookingsMono = dashboardUseCase.countCompletedBookingsByUser(userId);
        Mono<List<String>> mostBookedRoomsMono = calculateMostBookedRooms(userId);
        
        return Mono.zip(totalBookingsMono, upcomingBookingsMono, completedBookingsMono, mostBookedRoomsMono)
            .map(tuple -> new DashboardDTO.DashboardStats(
                tuple.getT1(),
                tuple.getT2(),
                tuple.getT3(),
                tuple.getT4()
            ));
    }
    
    private Mono<List<String>> calculateMostBookedRooms(Long userId) {
        return dashboardUseCase.getUserBookings(userId)
            .filter(booking -> "CONFIRMED".equals(booking.getStatus()))
            .flatMap(booking -> 
                meetingRoomUseCase.getRoomById(booking.getRoomId())
                    .map(room -> room.getName())
            )
            .collectList()
            .map(roomNames -> {
                // Count occurrences
                Map<String, Long> roomCounts = roomNames.stream()
                    .collect(Collectors.groupingBy(name -> name, Collectors.counting()));
                
                // Get top 3 most booked rooms
                return roomCounts.entrySet().stream()
                    .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                    .limit(3)
                    .map(Map.Entry::getKey)
                    .collect(Collectors.toList());
            });
    }
}
