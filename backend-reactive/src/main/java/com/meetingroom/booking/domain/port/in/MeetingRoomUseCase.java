package com.meetingroom.booking.domain.port.in;

import com.meetingroom.booking.domain.model.MeetingRoom;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Set;

/**
 * Input port for meeting room use cases
 */
public interface MeetingRoomUseCase {
    
    Mono<MeetingRoom> createRoom(MeetingRoom room);
    
    Mono<MeetingRoom> updateRoom(MeetingRoom room);
    
    Mono<Void> deleteRoom(Long id);
    
    Flux<MeetingRoom> getAllRooms();
    
    Flux<MeetingRoom> getAvailableRooms();
    
    Mono<MeetingRoom> getRoomById(Long id);
    
    /**
     * Batch fetch rooms by their IDs to avoid N+1 query problem
     * @param ids Set of room IDs to fetch
     * @return Flux of rooms matching the IDs
     */
    Flux<MeetingRoom> getRoomsByIds(Set<Long> ids);
}
