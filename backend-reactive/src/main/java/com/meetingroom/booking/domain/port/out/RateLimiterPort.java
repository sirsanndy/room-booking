package com.meetingroom.booking.domain.port.out;

import reactor.core.publisher.Mono;

/**
 * Output port for rate limiting operations
 */
public interface RateLimiterPort {
    
    Mono<Boolean> allowRequest(String key);
    
    Mono<Integer> getRemainingTokens(String key);
    
    Mono<Long> getTimeUntilRefill(String key);
    
    Mono<Void> clearRateLimit(String key);
}
