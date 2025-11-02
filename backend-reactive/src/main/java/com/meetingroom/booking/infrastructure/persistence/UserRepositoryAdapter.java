package com.meetingroom.booking.infrastructure.persistence;

import com.meetingroom.booking.domain.model.User;
import com.meetingroom.booking.domain.port.out.UserRepository;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
public class UserRepositoryAdapter implements UserRepository {
    
    private final R2dbcUserRepository r2dbcUserRepository;
    private final R2dbcEntityTemplate template;
    
    public UserRepositoryAdapter(R2dbcUserRepository r2dbcUserRepository,
                                R2dbcEntityTemplate template) {
        this.r2dbcUserRepository = r2dbcUserRepository;
        this.template = template;
    }
    
    @Override
    public Mono<User> save(User user) {
        return r2dbcUserRepository.save(user)
            .flatMap(savedUser -> {
                // Save roles separately
                if (savedUser.getRoles() != null && !savedUser.getRoles().isEmpty()) {
                    return saveUserRoles(savedUser.getId(), savedUser.getRoles())
                        .thenReturn(savedUser);
                }
                return Mono.just(savedUser);
            });
    }
    
    @Override
    public Mono<User> findById(Long id) {
        return r2dbcUserRepository.findById(id)
            .flatMap(this::loadUserRoles);
    }
    
    @Override
    public Mono<User> findByUsername(String username) {
        return r2dbcUserRepository.findByUsername(username)
            .flatMap(this::loadUserRoles);
    }
    
    @Override
    public Mono<Boolean> existsByUsername(String username) {
        return r2dbcUserRepository.existsByUsername(username);
    }
    
    @Override
    public Mono<Boolean> existsByEmail(String email) {
        return r2dbcUserRepository.existsByEmail(email);
    }
    
    private Mono<User> loadUserRoles(User user) {
        return r2dbcUserRepository.findRolesByUserId(user.getId())
            .collectList()
            .map(roles -> {
                user.getRoles().addAll(roles);
                return user;
            });
    }
    
    private Mono<Void> saveUserRoles(Long userId, java.util.Set<String> roles) {
        return template.getDatabaseClient()
            .sql("DELETE FROM user_roles WHERE user_id = :userId")
            .bind("userId", userId)
            .then()
            .thenMany(reactor.core.publisher.Flux.fromIterable(roles)
                .flatMap(role -> template.getDatabaseClient()
                    .sql("INSERT INTO user_roles (user_id, role) VALUES (:userId, :role)")
                    .bind("userId", userId)
                    .bind("role", role)
                    .then()
                )
            )
            .then();
    }
}
