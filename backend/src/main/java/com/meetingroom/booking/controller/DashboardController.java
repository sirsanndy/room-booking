package com.meetingroom.booking.controller;

import com.meetingroom.booking.dto.DashboardDTO;
import com.meetingroom.booking.service.DashboardService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/dashboard")
@CrossOrigin(origins = "*", maxAge = 3600)
public class DashboardController {
    private static final Logger LOG = LoggerFactory.getLogger(DashboardController.class);
    
    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping
    public ResponseEntity<DashboardDTO> getDashboard(Authentication authentication) {
        try {
            String username = authentication.getName();
            LOG.info("Fetching dashboard data for user: {}", username);
            DashboardDTO dashboard = dashboardService.getDashboardData(username);
            LOG.info("Dashboard data retrieved successfully for user: {} - {} events, {} total bookings", 
                username, dashboard.getEvents().size(), dashboard.getStats().getTotalBookings());
            return ResponseEntity.ok(dashboard);
        } catch (Exception e) {
            LOG.error("Failed to fetch dashboard data for user: {}", authentication.getName(), e);
            return ResponseEntity.internalServerError().build();
        }
    }
}
