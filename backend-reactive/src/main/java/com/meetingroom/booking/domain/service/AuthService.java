package com.meetingroom.booking.domain.service;

import com.meetingroom.booking.domain.model.User;
import com.meetingroom.booking.domain.port.in.AuthUseCase;
import com.meetingroom.booking.domain.port.out.CachePort;
import com.meetingroom.booking.domain.port.out.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.HashSet;
import java.util.Set;

@Service
public class AuthService implements AuthUseCase {
    private static final Logger LOG = LoggerFactory.getLogger(AuthService.class);
    private static final long USER_CACHE_TTL = 7200000; // 2 hours
    
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final CachePort cachePort;
    
    public AuthService(UserRepository userRepository, 
                      PasswordEncoder passwordEncoder,
                      JwtTokenProvider jwtTokenProvider,
                      CachePort cachePort) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenProvider = jwtTokenProvider;
        this.cachePort = cachePort;
    }
    
    @Override
    public Mono<User> registerUser(String username, String password, String email, String fullName) {
        LOG.info("Registering new user: {}", username);
        
        return userRepository.existsByUsername(username)
            .flatMap(exists -> {
                if (exists) {
                    return Mono.error(new RuntimeException("Username is already taken"));
                }
                return userRepository.existsByEmail(email);
            })
            .flatMap(emailExists -> {
                if (emailExists) {
                    return Mono.error(new RuntimeException("Email is already in use"));
                }
                
                User user = new User();
                user.setUsername(username);
                user.setEmail(email);
                user.setFullName(fullName);
                
                // Password received here is already SHA-256 hashed from frontend
                // BCrypt will hash it again with automatic salt generation
                // Final stored format: $2a$12$[salt][hash]
                // Each user gets unique salt even with same password
                user.setPassword(passwordEncoder.encode(password));
                
                Set<String> roles = new HashSet<>();
                roles.add("USER");
                user.setRoles(roles);
                user.setEnabled(true);
                
                return userRepository.save(user)
                    .doOnSuccess(savedUser -> LOG.info("User registered successfully with BCrypt-salted password: {}", username));
            });
    }
    
    @Override
    public Mono<String> authenticateUser(String username, String password) {
        LOG.info("Authenticating user: {}", username);
        
        // Password received here is SHA-256 hash from frontend
        // BCrypt.matches(sha256_hash, stored_bcrypt_hash) -> validates password
        return userRepository.findByUsername(username)
            .switchIfEmpty(Mono.error(new RuntimeException("Invalid username or password")))
            .flatMap(user -> {
                if (!passwordEncoder.matches(password, user.getPassword())) {
                    return Mono.error(new RuntimeException("Invalid username or password"));
                }
                
                if (!user.getEnabled()) {
                    return Mono.error(new RuntimeException("User account is disabled"));
                }
                
                String token = jwtTokenProvider.generateToken(username);
                LOG.info("User authenticated successfully: {}", username);
                
                return Mono.just(token);
            });
    }
    
    @Override
    public Mono<User> getUserByUsername(String username) {
        String cacheKey = "user:" + username;
        
        return cachePort.get(cacheKey, User.class)
            .switchIfEmpty(
                userRepository.findByUsername(username)
                    .flatMap(user -> 
                        cachePort.set(cacheKey, user, USER_CACHE_TTL)
                            .thenReturn(user)
                    )
            )
            .doOnSuccess(user -> {
                if (user != null) {
                    LOG.debug("User found: {}", username);
                }
            });
    }
    
    @Override
    public Mono<User> getUserById(Long id) {
        String cacheKey = "user:id:" + id;
        
        return cachePort.get(cacheKey, User.class)
            .switchIfEmpty(
                userRepository.findById(id)
                    .flatMap(user -> 
                        cachePort.set(cacheKey, user, USER_CACHE_TTL)
                            .thenReturn(user)
                    )
            )
            .doOnSuccess(user -> {
                if (user != null) {
                    LOG.debug("User found by id: {}", id);
                }
            });
    }
    
    @Override
    public Flux<User> getUsersByIds(Set<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            LOG.debug("No user IDs provided for batch fetch");
            return Flux.empty();
        }
        
        LOG.info("Batch fetching {} users by IDs", ids.size());
        return userRepository.findAllById(ids)
            .doOnComplete(() -> LOG.info("Batch fetch completed for {} users", ids.size()));
    }
}
