package com.meetingroom.booking.infrastructure.persistence;

import com.meetingroom.booking.domain.model.MeetingRoom;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;

@Repository
public interface R2dbcMeetingRoomRepository extends R2dbcRepository<MeetingRoom, Long> {
    
    Flux<MeetingRoom> findByAvailableTrue();
}
