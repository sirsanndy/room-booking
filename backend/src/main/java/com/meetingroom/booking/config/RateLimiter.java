package com.meetingroom.booking.config;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * Token Bucket Rate Limiter implementation using Redis
 * Limits requests per user to prevent abuse
 */
@Component
public class RateLimiter {
    
    private final RedisTemplate<String, Object> redisTemplate;
    
    // Rate limit configuration
    private static final int MAX_TOKENS = 30; // Maximum tokens in bucket
    private static final long REFILL_RATE_SECONDS = 60; // Refill all tokens every 60 seconds
    
    public RateLimiter(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }
    
    /**
     * Check if request is allowed for the given key (username + action)
     * @param key Unique identifier (e.g., "user:john:create_booking")
     * @return true if request is allowed, false if rate limit exceeded
     */
    public boolean allowRequest(String key) {
        String tokenKey = "rate_limit:" + key + ":tokens";
        String timestampKey = "rate_limit:" + key + ":timestamp";
        
        Object tokensObj = redisTemplate.opsForValue().get(tokenKey);
        Object timestampObj = redisTemplate.opsForValue().get(timestampKey);
        
        Long currentTokens = convertToLong(tokensObj);
        Long lastRefillTimestamp = convertToLong(timestampObj);
        
        long now = System.currentTimeMillis();        if (currentTokens == null || lastRefillTimestamp == null) {
            redisTemplate.opsForValue().set(tokenKey, (long) (MAX_TOKENS - 1), Duration.ofSeconds(REFILL_RATE_SECONDS + 10));
            redisTemplate.opsForValue().set(timestampKey, now, Duration.ofSeconds(REFILL_RATE_SECONDS + 10));
            return true;
        }
        
        long timeElapsed = now - lastRefillTimestamp;
        
        if (timeElapsed >= REFILL_RATE_SECONDS * 1000) {
            // Refill period has passed - reset to full bucket minus current request
            redisTemplate.opsForValue().set(tokenKey, (long) (MAX_TOKENS - 1), Duration.ofSeconds(REFILL_RATE_SECONDS + 10));
            redisTemplate.opsForValue().set(timestampKey, now, Duration.ofSeconds(REFILL_RATE_SECONDS + 10));
            return true;
        }
        
        if (currentTokens > 0) {
            redisTemplate.opsForValue().decrement(tokenKey);
            return true;
        }
        
        return false;
    }
    
    /**
     * Get remaining tokens for the given key
     * @param key Unique identifier
     * @return Number of remaining tokens, or MAX_TOKENS if not initialized
     */
    public int getRemainingTokens(String key) {
        String tokenKey = "rate_limit:" + key + ":tokens";
        Object tokensObj = redisTemplate.opsForValue().get(tokenKey);
        Long tokens = convertToLong(tokensObj);
        return tokens != null ? tokens.intValue() : MAX_TOKENS;
    }
    
    /**
     * Get time until token bucket refills (in seconds)
     * @param key Unique identifier
     * @return Seconds until refill, or 0 if no rate limit active
     */
    public long getTimeUntilRefill(String key) {
        String timestampKey = "rate_limit:" + key + ":timestamp";
        Object timestampObj = redisTemplate.opsForValue().get(timestampKey);
        Long lastRefillTimestamp = convertToLong(timestampObj);
        
        if (lastRefillTimestamp == null) {
            return 0;
        }
        
        long now = System.currentTimeMillis();
        long timeElapsed = now - lastRefillTimestamp;
        long timeRemaining = (REFILL_RATE_SECONDS * 1000) - timeElapsed;
        
        return Math.max(0, timeRemaining / 1000);
    }
    
    /**
     * Clear rate limit for a specific key (useful for testing or admin reset)
     * @param key Unique identifier
     */
    public void clearRateLimit(String key) {
        String tokenKey = "rate_limit:" + key + ":tokens";
        String timestampKey = "rate_limit:" + key + ":timestamp";
        redisTemplate.delete(tokenKey);
        redisTemplate.delete(timestampKey);
    }
    
    /**
     * Safely convert Redis Object to Long
     * Handles both Integer and Long types that Redis might return
     * @param obj Object from Redis
     * @return Long value or null if conversion fails
     */
    private Long convertToLong(Object obj) {
        if (obj == null) {
            return null;
        }
        if (obj instanceof Long) {
            return (Long) obj;
        }
        if (obj instanceof Integer) {
            return ((Integer) obj).longValue();
        }
        if (obj instanceof String) {
            try {
                return Long.parseLong((String) obj);
            } catch (NumberFormatException e) {
                return null;
            }
        }
        if (obj instanceof Number) {
            return ((Number) obj).longValue();
        }
        return null;
    }
}
