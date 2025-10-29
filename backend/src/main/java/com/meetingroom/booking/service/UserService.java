package com.meetingroom.booking.service;

import com.meetingroom.booking.entity.User;
import com.meetingroom.booking.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {
    private static final Logger LOG = LoggerFactory.getLogger(UserService.class);
    
    private final UserRepository userRepository;
    
    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    @Cacheable(value = "users", key = "#username")
    public User findByUsername(String username) {
        LOG.info("Fetching user by username: {} (cache miss)", username);
        return userRepository.findByUsername(username)
                .orElseThrow(() -> {
                    LOG.error("User not found: {}", username);
                    return new RuntimeException("User not found: " + username);
                });
    }
}
