package com.meetingroom.booking.controller;

import com.meetingroom.booking.dto.DashboardDTO;
import com.meetingroom.booking.service.DashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/dashboard")
@CrossOrigin(origins = "*", maxAge = 3600)
@Tag(name = "Dashboard", description = "Dashboard data endpoints")
@SecurityRequirement(name = "Bearer Authentication")
public class DashboardController {
    private static final Logger LOG = LoggerFactory.getLogger(DashboardController.class);
    
    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping
    @Operation(summary = "Get dashboard data", description = "Get dashboard data including calendar events and statistics")
    @ApiResponse(responseCode = "200", description = "Dashboard data retrieved successfully",
        content = @Content(mediaType = "application/json", schema = @Schema(implementation = DashboardDTO.class)))
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
