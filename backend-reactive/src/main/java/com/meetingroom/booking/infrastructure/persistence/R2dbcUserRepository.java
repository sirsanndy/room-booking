package com.meetingroom.booking.infrastructure.persistence;

import com.meetingroom.booking.domain.model.User;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Repository
public interface R2dbcUserRepository extends R2dbcRepository<User, Long> {
    
    Mono<User> findByUsername(String username);
    
    Mono<Boolean> existsByUsername(String username);
    
    Mono<Boolean> existsByEmail(String email);
    
    @Query("SELECT r.role FROM user_roles r WHERE r.user_id = :userId")
    Flux<String> findRolesByUserId(Long userId);
}
