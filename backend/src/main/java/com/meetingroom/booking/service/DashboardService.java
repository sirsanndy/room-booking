package com.meetingroom.booking.service;

import com.meetingroom.booking.dto.BookingResponse;
import com.meetingroom.booking.dto.CalendarEventDTO;
import com.meetingroom.booking.dto.DashboardDTO;
import com.meetingroom.booking.entity.Booking;
import com.meetingroom.booking.entity.Holiday;
import com.meetingroom.booking.entity.User;
import com.meetingroom.booking.repository.BookingRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class DashboardService {
    private static final Logger LOG = LoggerFactory.getLogger(DashboardService.class);
    
    private final BookingRepository bookingRepository;
    private final BookingService bookingService;
    private final UserService userService;
    private final HolidayService holidayService;

    public DashboardService(BookingRepository bookingRepository, 
                          BookingService bookingService,
                          UserService userService,
                          HolidayService holidayService) {
        this.bookingRepository = bookingRepository;
        this.bookingService = bookingService;
        this.userService = userService;
        this.holidayService = holidayService;
    }

    @Transactional(readOnly = true)
    @Cacheable(value = "dashboardData", key = "#username")
    public DashboardDTO getDashboardData(String username) {
        LOG.info("Fetching dashboard data for user: {} (cache miss)", username);
        
        // Get user (from cache if available)
        User user = userService.findByUsername(username);
        
        // Use cached method to get all upcoming bookings
        // This will check Redis cache first before hitting the database
        List<BookingResponse> allUpcomingBookings = bookingService.getAllUpcomingBookings();
        LOG.info("Fetched {} upcoming bookings from cache/database", allUpcomingBookings.size());
        
        // Get user's bookings for stats calculation
        List<Booking> userBookings = bookingRepository.findBookingsByUser(user.getId());
        LOG.info("Found {} total bookings for user: {}", userBookings.size(), username);
        
        // Get upcoming holidays (next 3 months) - from cache if available
        LocalDate today = LocalDate.now();
        LocalDate threeMonthsLater = today.plusMonths(3);
        List<Holiday> upcomingHolidays = holidayService.findHolidaysBetween(today, threeMonthsLater);
        LOG.info("Found {} upcoming holidays", upcomingHolidays.size());
        
        // Convert to calendar events
        List<CalendarEventDTO> events = new ArrayList<>();
        
        // Add all upcoming bookings (from cache)
        for (BookingResponse booking : allUpcomingBookings) {
            CalendarEventDTO event = new CalendarEventDTO();
            event.setId(booking.getId());
            event.setTitle(booking.getTitle());
            event.setStart(booking.getStartTime());
            event.setEnd(booking.getEndTime());
            event.setDescription(booking.getDescription());
            event.setRoomName(booking.getRoomName());
            event.setRoomId(booking.getRoomId());
            event.setStatus(booking.getStatus());
            event.setType("booking");
            
            // Set color based on status
            if ("CONFIRMED".equals(booking.getStatus())) {
                event.setColor("#3498db"); // Blue
            } else {
                event.setColor("#95a5a6"); // Gray for cancelled
            }
            
            events.add(event);
        }
        
        // Add holidays
        for (Holiday holiday : upcomingHolidays) {
            CalendarEventDTO event = new CalendarEventDTO();
            event.setId(holiday.getId());
            event.setTitle(holiday.getName());
            event.setStart(holiday.getDate().atStartOfDay());
            event.setEnd(holiday.getDate().atTime(23, 59));
            event.setDescription(holiday.getDescription());
            event.setType("holiday");
            event.setColor("#e74c3c"); // Red for holidays
            events.add(event);
        }
        
        DashboardDTO.DashboardStats stats = calculateStats(userBookings);
        
        LOG.info("Dashboard data prepared with {} events", events.size());
        return new DashboardDTO(events, stats);
    }
    
    private DashboardDTO.DashboardStats calculateStats(List<Booking> bookings) {
        LocalDateTime now = LocalDateTime.now();
        
        long totalBookings = bookings.stream()
                .filter(b -> b.getStatus().name().equals("CONFIRMED"))
                .count();
        
        long upcomingBookings = bookings.stream()
                .filter(b -> b.getStatus().name().equals("CONFIRMED") && b.getStartTime().isAfter(now))
                .count();
        
        long completedBookings = bookings.stream()
                .filter(b -> b.getStatus().name().equals("CONFIRMED") && b.getEndTime().isBefore(now))
                .count();
        
        Map<String, Long> roomBookingCounts = bookings.stream()
                .filter(b -> b.getStatus().name().equals("CONFIRMED"))
                .collect(Collectors.groupingBy(
                        b -> b.getRoom().getName(),
                        Collectors.counting()
                ));
        
        List<String> mostBookedRooms = roomBookingCounts.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(3)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
        
        return new DashboardDTO.DashboardStats(
                totalBookings,
                upcomingBookings,
                completedBookings,
                mostBookedRooms
        );
    }
}
