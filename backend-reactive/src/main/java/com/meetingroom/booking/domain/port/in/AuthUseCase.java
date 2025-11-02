package com.meetingroom.booking.domain.port.in;

import com.meetingroom.booking.domain.model.User;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Set;

/**
 * Input port for authentication use cases
 */
public interface AuthUseCase {
    
    Mono<User> registerUser(String username, String password, String email, String fullName);
    
    Mono<String> authenticateUser(String username, String password);
    
    Mono<User> getUserByUsername(String username);
    
    /**
     * Get user by ID
     * @param id User ID
     * @return Mono of User
     */
    Mono<User> getUserById(Long id);
    
    /**
     * Batch fetch users by their IDs to avoid N+1 query problem
     * @param ids Set of user IDs to fetch
     * @return Flux of users matching the IDs
     */
    Flux<User> getUsersByIds(Set<Long> ids);
}
