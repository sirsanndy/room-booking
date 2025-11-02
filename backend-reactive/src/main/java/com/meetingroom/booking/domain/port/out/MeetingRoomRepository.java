package com.meetingroom.booking.domain.port.out;

import com.meetingroom.booking.domain.model.MeetingRoom;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Output port for meeting room persistence
 */
public interface MeetingRoomRepository {
    
    Mono<MeetingRoom> save(MeetingRoom room);
    
    Mono<MeetingRoom> findById(Long id);
    
    Flux<MeetingRoom> findAll();
    
    Flux<MeetingRoom> findByAvailableTrue();
    
    Mono<Void> deleteById(Long id);
}
