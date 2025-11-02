package com.meetingroom.booking.domain.port.out;

import reactor.core.publisher.Mono;

/**
 * Output port for caching operations
 */
public interface CachePort {
    
    <T> Mono<T> get(String key, Class<T> type);
    
    <T> Mono<Boolean> set(String key, T value, long ttlMillis);
    
    Mono<Boolean> delete(String key);
    
    Mono<Boolean> exists(String key);
}
