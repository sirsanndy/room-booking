package com.meetingroom.booking.domain.port.out;

import com.meetingroom.booking.domain.model.User;
import reactor.core.publisher.Mono;

/**
 * Output port for user persistence
 */
public interface UserRepository {
    
    Mono<User> save(User user);
    
    Mono<User> findById(Long id);
    
    Mono<User> findByUsername(String username);
    
    Mono<Boolean> existsByUsername(String username);
    
    Mono<Boolean> existsByEmail(String email);
}
