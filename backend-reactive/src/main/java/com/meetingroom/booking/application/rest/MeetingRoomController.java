package com.meetingroom.booking.application.rest;

import com.meetingroom.booking.application.dto.MeetingRoomResponse;
import com.meetingroom.booking.domain.model.MeetingRoom;
import com.meetingroom.booking.domain.port.in.MeetingRoomUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/rooms")
@CrossOrigin(origins = "*", maxAge = 3600)
@Tag(name = "Meeting Rooms", description = "Meeting room management endpoints (Reactive)")
@SecurityRequirement(name = "Bearer Authentication")
public class MeetingRoomController {
    private static final Logger LOG = LoggerFactory.getLogger(MeetingRoomController.class);
    
    private final MeetingRoomUseCase meetingRoomUseCase;
    
    public MeetingRoomController(MeetingRoomUseCase meetingRoomUseCase) {
        this.meetingRoomUseCase = meetingRoomUseCase;
    }
    
    @GetMapping
    @Operation(summary = "Get all rooms", description = "Retrieve all meeting rooms")
    @ApiResponse(responseCode = "200", description = "Rooms retrieved successfully")
    public Flux<MeetingRoomResponse> getAllRooms() {
        LOG.info("Fetching all rooms");
        return meetingRoomUseCase.getAllRooms()
            .map(this::mapToResponse);
    }
    
    @GetMapping("/available")
    @Operation(summary = "Get available rooms", description = "Retrieve all available meeting rooms")
    @ApiResponse(responseCode = "200", description = "Available rooms retrieved successfully")
    public Flux<MeetingRoomResponse> getAvailableRooms() {
        LOG.info("Fetching available rooms");
        return meetingRoomUseCase.getAvailableRooms()
            .map(this::mapToResponse);
    }
    
    @GetMapping("/{id}")
    @Operation(summary = "Get room by ID", description = "Retrieve a specific meeting room by ID")
    @ApiResponse(responseCode = "200", description = "Room found")
    @ApiResponse(responseCode = "404", description = "Room not found")
    public Mono<MeetingRoomResponse> getRoomById(@PathVariable Long id) {
        LOG.info("Fetching room by id: {}", id);
        return meetingRoomUseCase.getRoomById(id)
            .map(this::mapToResponse);
    }
    
    private MeetingRoomResponse mapToResponse(MeetingRoom room) {
        MeetingRoomResponse response = new MeetingRoomResponse();
        response.setId(room.getId());
        response.setName(room.getName());
        response.setCapacity(room.getCapacity());
        response.setDescription(room.getDescription());
        response.setLocation(room.getLocation());
        response.setAvailable(room.getAvailable());
        response.setFacilities(room.getFacilities());
        response.setCreatedAt(room.getCreatedAt());
        return response;
    }
}
