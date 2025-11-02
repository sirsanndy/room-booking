package com.meetingroom.booking.infrastructure.persistence;

import com.meetingroom.booking.domain.model.MeetingRoom;
import com.meetingroom.booking.domain.port.out.MeetingRoomRepository;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Component
public class MeetingRoomRepositoryAdapter implements MeetingRoomRepository {
    
    private final R2dbcMeetingRoomRepository r2dbcMeetingRoomRepository;
    
    public MeetingRoomRepositoryAdapter(R2dbcMeetingRoomRepository r2dbcMeetingRoomRepository) {
        this.r2dbcMeetingRoomRepository = r2dbcMeetingRoomRepository;
    }
    
    @Override
    public Mono<MeetingRoom> save(MeetingRoom room) {
        return r2dbcMeetingRoomRepository.save(room);
    }
    
    @Override
    public Mono<MeetingRoom> findById(Long id) {
        return r2dbcMeetingRoomRepository.findById(id);
    }
    
    @Override
    public Flux<MeetingRoom> findAll() {
        return r2dbcMeetingRoomRepository.findAll();
    }
    
    @Override
    public Flux<MeetingRoom> findByAvailableTrue() {
        return r2dbcMeetingRoomRepository.findByAvailableTrue();
    }
    
    @Override
    public Mono<Void> deleteById(Long id) {
        return r2dbcMeetingRoomRepository.deleteById(id);
    }
}
