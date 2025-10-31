package com.meetingroom.booking.controller;

import com.meetingroom.booking.dto.MeetingRoomResponse;
import com.meetingroom.booking.service.MeetingRoomService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/rooms")
@CrossOrigin(origins = "*", maxAge = 3600)
@Tag(name = "Meeting Rooms", description = "Meeting room management endpoints")
@SecurityRequirement(name = "Bearer Authentication")
public class MeetingRoomController {
    private static final Logger LOG = LoggerFactory.getLogger(MeetingRoomController.class);
    private final MeetingRoomService meetingRoomService;

    public MeetingRoomController(MeetingRoomService meetingRoomService) {
        this.meetingRoomService = meetingRoomService;
    }

    @GetMapping
    @Operation(summary = "Get all rooms", description = "Retrieve all meeting rooms")
    @ApiResponse(responseCode = "200", description = "Rooms retrieved successfully")
    public ResponseEntity<List<MeetingRoomResponse>> getAllRooms() {
        return ResponseEntity.ok(meetingRoomService.getAllRooms());
    }
    
    @GetMapping("/available")
    @Operation(summary = "Get available rooms", description = "Retrieve all available meeting rooms")
    @ApiResponse(responseCode = "200", description = "Available rooms retrieved successfully")
    public ResponseEntity<List<MeetingRoomResponse>> getAvailableRooms() {
        LOG.info("getAvailableRooms Request");
        return ResponseEntity.ok(meetingRoomService.getAvailableRooms());
    }
    
    @GetMapping("/{id}")
    @Operation(summary = "Get room by ID", description = "Retrieve a specific meeting room by ID")
    @ApiResponse(responseCode = "200", description = "Room found")
    @ApiResponse(responseCode = "404", description = "Room not found")
    public ResponseEntity<?> getRoomById(@PathVariable Long id) {
        LOG.info("getRoomById Request {}", id);
        try {
            MeetingRoomResponse room = meetingRoomService.getRoomById(id);
            return ResponseEntity.ok(room);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }
}
