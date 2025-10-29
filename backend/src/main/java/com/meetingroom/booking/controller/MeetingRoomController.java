package com.meetingroom.booking.controller;

import com.meetingroom.booking.dto.MeetingRoomResponse;
import com.meetingroom.booking.service.MeetingRoomService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/rooms")
@CrossOrigin(origins = "*", maxAge = 3600)
public class MeetingRoomController {
    private static final Logger LOG = LoggerFactory.getLogger(MeetingRoomController.class);
    private final MeetingRoomService meetingRoomService;

    public MeetingRoomController(MeetingRoomService meetingRoomService) {
        this.meetingRoomService = meetingRoomService;
    }

    @GetMapping
    public ResponseEntity<List<MeetingRoomResponse>> getAllRooms() {
        return ResponseEntity.ok(meetingRoomService.getAllRooms());
    }
    
    @GetMapping("/available")
    public ResponseEntity<List<MeetingRoomResponse>> getAvailableRooms() {
        LOG.info("getAvailableRooms Request");
        return ResponseEntity.ok(meetingRoomService.getAvailableRooms());
    }
    
    @GetMapping("/{id}")
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
