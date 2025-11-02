package com.meetingroom.booking.infrastructure.cache;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.meetingroom.booking.domain.port.out.CachePort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.time.Duration;

@Component
public class RedisCacheAdapter implements CachePort {
    private static final Logger LOG = LoggerFactory.getLogger(RedisCacheAdapter.class);
    
    private final ReactiveRedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;
    
    public RedisCacheAdapter(ReactiveRedisTemplate<String, String> redisTemplate,
                            ObjectMapper objectMapper) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }
    
    @Override
    public <T> Mono<T> get(String key, Class<T> type) {
        return redisTemplate.opsForValue()
            .get(key)
            .flatMap(value -> {
                try {
                    T result = objectMapper.readValue(value, type);
                    LOG.debug("Cache hit for key: {}", key);
                    return Mono.just(result);
                } catch (JsonProcessingException e) {
                    LOG.error("Error deserializing cached value for key: {}", key, e);
                    return Mono.empty();
                }
            })
            .doOnError(error -> LOG.error("Error getting from cache for key: {}", key, error))
            .onErrorResume(error -> Mono.empty());
    }
    
    @Override
    public <T> Mono<Boolean> set(String key, T value, long ttlMillis) {
        try {
            String jsonValue = objectMapper.writeValueAsString(value);
            return redisTemplate.opsForValue()
                .set(key, jsonValue, Duration.ofMillis(ttlMillis))
                .doOnSuccess(success -> {
                    if (success) {
                        LOG.debug("Cache set for key: {}", key);
                    }
                })
                .doOnError(error -> LOG.error("Error setting cache for key: {}", key, error))
                .onErrorReturn(false);
        } catch (JsonProcessingException e) {
            LOG.error("Error serializing value for key: {}", key, e);
            return Mono.just(false);
        }
    }
    
    @Override
    public Mono<Boolean> delete(String key) {
        if (key.endsWith("*")) {
            // Handle wildcard deletion
            String pattern = key;
            return redisTemplate.keys(pattern)
                .flatMap(redisTemplate::delete)
                .reduce(0L, Long::sum)
                .map(count -> count > 0)
                .doOnSuccess(success -> LOG.debug("Deleted {} keys matching pattern: {}", success, pattern))
                .doOnError(error -> LOG.error("Error deleting keys with pattern: {}", pattern, error))
                .onErrorReturn(false);
        } else {
            return redisTemplate.delete(key)
                .map(count -> count > 0)
                .doOnSuccess(success -> LOG.debug("Cache deleted for key: {}", key))
                .doOnError(error -> LOG.error("Error deleting cache for key: {}", key, error))
                .onErrorReturn(false);
        }
    }
    
    @Override
    public Mono<Boolean> exists(String key) {
        return redisTemplate.hasKey(key)
            .doOnError(error -> LOG.error("Error checking existence for key: {}", key, error))
            .onErrorReturn(false);
    }
}
