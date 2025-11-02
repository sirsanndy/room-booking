package com.meetingroom.booking.infrastructure.ratelimiter;

import com.meetingroom.booking.domain.port.out.RateLimiterPort;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.time.Duration;

@Component
public class RedisRateLimiterAdapter implements RateLimiterPort {
    
    private final ReactiveRedisTemplate<String, String> redisTemplate;
    private final int maxTokens;
    private final long refillRateSeconds;
    
    public RedisRateLimiterAdapter(ReactiveRedisTemplate<String, String> redisTemplate,
                                  @Value("${rate.limiter.max.tokens:30}") int maxTokens,
                                  @Value("${rate.limiter.refill.rate.seconds:60}") long refillRateSeconds) {
        this.redisTemplate = redisTemplate;
        this.maxTokens = maxTokens;
        this.refillRateSeconds = refillRateSeconds;
    }
    
    @Override
    public Mono<Boolean> allowRequest(String key) {
        String tokenKey = "rate_limit:" + key + ":tokens";
        String timestampKey = "rate_limit:" + key + ":timestamp";
        
        long now = System.currentTimeMillis();
        
        return redisTemplate.opsForValue().get(tokenKey)
            .zipWith(redisTemplate.opsForValue().get(timestampKey))
            .flatMap(tuple -> {
                Long currentTokens = parseLong(tuple.getT1());
                Long lastRefillTimestamp = parseLong(tuple.getT2());
                
                if (currentTokens == null || lastRefillTimestamp == null) {
                    return initializeRateLimit(tokenKey, timestampKey, now);
                }
                
                long timeElapsed = now - lastRefillTimestamp;
                
                if (timeElapsed >= refillRateSeconds * 1000) {
                    return refillAndConsume(tokenKey, timestampKey, now);
                }
                
                if (currentTokens > 0) {
                    return redisTemplate.opsForValue()
                        .decrement(tokenKey)
                        .thenReturn(true);
                }
                
                return Mono.just(false);
            })
            .switchIfEmpty(initializeRateLimit(tokenKey, timestampKey, now))
            .onErrorReturn(false);
    }
    
    @Override
    public Mono<Integer> getRemainingTokens(String key) {
        String tokenKey = "rate_limit:" + key + ":tokens";
        
        return redisTemplate.opsForValue()
            .get(tokenKey)
            .map(value -> {
                Long tokens = parseLong(value);
                return tokens != null ? tokens.intValue() : maxTokens;
            })
            .defaultIfEmpty(maxTokens)
            .onErrorReturn(maxTokens);
    }
    
    @Override
    public Mono<Long> getTimeUntilRefill(String key) {
        String timestampKey = "rate_limit:" + key + ":timestamp";
        
        return redisTemplate.opsForValue()
            .get(timestampKey)
            .map(value -> {
                Long lastRefillTimestamp = parseLong(value);
                if (lastRefillTimestamp == null) {
                    return 0L;
                }
                
                long now = System.currentTimeMillis();
                long timeElapsed = now - lastRefillTimestamp;
                long timeRemaining = (refillRateSeconds * 1000) - timeElapsed;
                
                return Math.max(0, timeRemaining / 1000);
            })
            .defaultIfEmpty(0L)
            .onErrorReturn(0L);
    }
    
    @Override
    public Mono<Void> clearRateLimit(String key) {
        String tokenKey = "rate_limit:" + key + ":tokens";
        String timestampKey = "rate_limit:" + key + ":timestamp";
        
        return redisTemplate.delete(tokenKey)
            .then(redisTemplate.delete(timestampKey))
            .then();
    }
    
    private Mono<Boolean> initializeRateLimit(String tokenKey, String timestampKey, long now) {
        return redisTemplate.opsForValue()
            .set(tokenKey, String.valueOf(maxTokens - 1), 
                Duration.ofSeconds(refillRateSeconds + 10))
            .then(redisTemplate.opsForValue()
                .set(timestampKey, String.valueOf(now), 
                    Duration.ofSeconds(refillRateSeconds + 10))
            )
            .thenReturn(true);
    }
    
    private Mono<Boolean> refillAndConsume(String tokenKey, String timestampKey, long now) {
        return redisTemplate.opsForValue()
            .set(tokenKey, String.valueOf(maxTokens - 1), 
                Duration.ofSeconds(refillRateSeconds + 10))
            .then(redisTemplate.opsForValue()
                .set(timestampKey, String.valueOf(now), 
                    Duration.ofSeconds(refillRateSeconds + 10))
            )
            .thenReturn(true);
    }
    
    private Long parseLong(String value) {
        if (value == null) {
            return null;
        }
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
