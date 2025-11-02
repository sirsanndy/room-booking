package com.meetingroom.booking.domain.service;

import com.meetingroom.booking.domain.model.MeetingRoom;
import com.meetingroom.booking.domain.port.in.MeetingRoomUseCase;
import com.meetingroom.booking.domain.port.out.CachePort;
import com.meetingroom.booking.domain.port.out.MeetingRoomRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Set;

@Service
public class MeetingRoomService implements MeetingRoomUseCase {
    private static final Logger LOG = LoggerFactory.getLogger(MeetingRoomService.class);
    private static final long ROOM_CACHE_TTL = 3600000; // 1 hour
    
    private final MeetingRoomRepository meetingRoomRepository;
    private final CachePort cachePort;
    
    public MeetingRoomService(MeetingRoomRepository meetingRoomRepository,
                             CachePort cachePort) {
        this.meetingRoomRepository = meetingRoomRepository;
        this.cachePort = cachePort;
    }
    
    @Override
    public Mono<MeetingRoom> createRoom(MeetingRoom room) {
        LOG.info("Creating meeting room: {}", room.getName());
        return meetingRoomRepository.save(room)
            .flatMap(savedRoom -> clearRoomCaches().thenReturn(savedRoom))
            .doOnSuccess(savedRoom -> LOG.info("Room created successfully: {}", savedRoom.getId()));
    }
    
    @Override
    public Mono<MeetingRoom> updateRoom(MeetingRoom room) {
        LOG.info("Updating meeting room: {}", room.getId());
        return meetingRoomRepository.save(room)
            .flatMap(updatedRoom -> clearRoomCaches().thenReturn(updatedRoom))
            .doOnSuccess(updatedRoom -> LOG.info("Room updated successfully: {}", updatedRoom.getId()));
    }
    
    @Override
    public Mono<Void> deleteRoom(Long id) {
        LOG.info("Deleting meeting room: {}", id);
        return meetingRoomRepository.deleteById(id)
            .then(clearRoomCaches())
            .doOnSuccess(v -> LOG.info("Room deleted successfully: {}", id));
    }
    
    @Override
    public Flux<MeetingRoom> getAllRooms() {
        String cacheKey = "rooms";
        
        return cachePort.get(cacheKey, MeetingRoom[].class)
            .flatMapMany(rooms -> Flux.fromArray(rooms))
            .switchIfEmpty(
                meetingRoomRepository.findAll()
                    .collectList()
                    .flatMap(roomList -> {
                        if (!roomList.isEmpty()) {
                            MeetingRoom[] roomsArray = roomList.toArray(new MeetingRoom[0]);
                            return cachePort.set(cacheKey, roomsArray, ROOM_CACHE_TTL)
                                .thenReturn(roomsArray);
                        }
                        return Mono.just(new MeetingRoom[0]);
                    })
                    .flatMapMany(Flux::fromArray)
            )
            .doOnSubscribe(s -> LOG.info("Fetching all rooms"));
    }
    
    @Override
    public Flux<MeetingRoom> getAvailableRooms() {
        String cacheKey = "availableRooms";
        
        return cachePort.get(cacheKey, MeetingRoom[].class)
            .flatMapMany(rooms -> Flux.fromArray(rooms))
            .switchIfEmpty(
                meetingRoomRepository.findByAvailableTrue()
                    .collectList()
                    .flatMap(roomList -> {
                        if (!roomList.isEmpty()) {
                            MeetingRoom[] roomsArray = roomList.toArray(new MeetingRoom[0]);
                            return cachePort.set(cacheKey, roomsArray, ROOM_CACHE_TTL)
                                .thenReturn(roomsArray);
                        }
                        return Mono.just(new MeetingRoom[0]);
                    })
                    .flatMapMany(Flux::fromArray)
            )
            .doOnSubscribe(s -> LOG.info("Fetching available rooms"));
    }
    
    @Override
    public Mono<MeetingRoom> getRoomById(Long id) {
        String cacheKey = "meetingRooms:" + id;
        
        return cachePort.get(cacheKey, MeetingRoom.class)
            .switchIfEmpty(
                meetingRoomRepository.findById(id)
                    .flatMap(room -> 
                        cachePort.set(cacheKey, room, ROOM_CACHE_TTL)
                            .thenReturn(room)
                    )
            )
            .doOnSubscribe(s -> LOG.info("Fetching room by id: {}", id));
    }
    
    @Override
    public Flux<MeetingRoom> getRoomsByIds(Set<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            LOG.debug("No room IDs provided for batch fetch");
            return Flux.empty();
        }
        
        LOG.info("Batch fetching {} rooms by IDs", ids.size());
        return meetingRoomRepository.findAllById(ids)
            .doOnComplete(() -> LOG.info("Batch fetch completed for {} rooms", ids.size()));
    }
    
    private Mono<Void> clearRoomCaches() {
        return Mono.when(
            cachePort.delete("rooms"),
            cachePort.delete("availableRooms")
        );
    }
}
