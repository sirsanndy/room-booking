package com.meetingroom.booking.service;

import com.meetingroom.booking.dto.MeetingRoomResponse;
import com.meetingroom.booking.entity.MeetingRoom;
import com.meetingroom.booking.repository.MeetingRoomRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class MeetingRoomService {
    private static final Logger LOG = LoggerFactory.getLogger(MeetingRoomService.class);
    private final MeetingRoomRepository meetingRoomRepository;

    public MeetingRoomService(MeetingRoomRepository meetingRoomRepository) {
        this.meetingRoomRepository = meetingRoomRepository;
    }

    @Transactional
    @CacheEvict(value = {"rooms", "availableRooms"}, allEntries = true)
    public void addRoom(MeetingRoom room) {
        meetingRoomRepository.save(room);
    }

    @Transactional
    @CacheEvict(value = {"rooms", "availableRooms"}, allEntries = true)
    public void updateRoom(MeetingRoom room) {
        meetingRoomRepository.save(room);
    }

    @Transactional
    @CacheEvict(value = {"rooms", "availableRooms"}, allEntries = true)
    public void deleteRoom(Long id) {
        meetingRoomRepository.deleteById(id);
    }

    @Transactional(readOnly = true)
    @Cacheable(value = "rooms")
    public List<MeetingRoomResponse> getAllRooms() {
        LOG.info("getAllRooms from database (cache miss)");
        return meetingRoomRepository.findAll()
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }
    
    @Transactional(readOnly = true)
    @Cacheable(value = "availableRooms")
    public List<MeetingRoomResponse> getAvailableRooms() {
        LOG.info("getAvailableRooms from database (cache miss)");
        return meetingRoomRepository.findByAvailableTrue()
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }
    
    @Transactional(readOnly = true)
    @Cacheable(value = "meetingRooms", key = "#id")
    public MeetingRoomResponse getRoomById(Long id) {
        LOG.info("getRoomById from database with id {} (cache miss)", id);
        MeetingRoom room = meetingRoomRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Meeting room not found"));
        return mapToResponse(room);
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
