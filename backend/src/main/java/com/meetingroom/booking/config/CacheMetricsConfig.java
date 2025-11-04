package com.meetingroom.booking.config;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Configuration to track cache metrics for Redis using AOP
 * Intercepts @Cacheable and @CacheEvict methods to record hits/misses
 */
@Configuration
public class CacheMetricsConfig {

    /**
     * Aspect to intercept cache operations and record metrics
     */
    @Aspect
    @Component
    public static class CacheMetricsAspect {

        private final MeterRegistry meterRegistry;
        private final CacheManager cacheManager;
        private final Map<String, Counter> hitCounters = new ConcurrentHashMap<>();
        private final Map<String, Counter> missCounters = new ConcurrentHashMap<>();
        private final Map<String, Counter> putCounters = new ConcurrentHashMap<>();
        private final Map<String, Counter> evictionCounters = new ConcurrentHashMap<>();

        public CacheMetricsAspect(MeterRegistry meterRegistry, CacheManager cacheManager) {
            this.meterRegistry = meterRegistry;
            this.cacheManager = cacheManager;
        }

        @Around("@annotation(org.springframework.cache.annotation.Cacheable)")
        public Object aroundCacheable(ProceedingJoinPoint joinPoint) throws Throwable {
            MethodSignature signature = (MethodSignature) joinPoint.getSignature();
            Method method = signature.getMethod();
            Cacheable cacheable = method.getAnnotation(Cacheable.class);

            if (cacheable != null && cacheable.value().length > 0) {
                String cacheName = cacheable.value()[0];
                
                // Generate cache key
                String cacheKey = generateCacheKey(joinPoint);
                
                // Check if value exists in cache BEFORE method execution
                Cache cache = cacheManager.getCache(cacheName);
                boolean isHit = false;
                
                if (cache != null) {
                    Cache.ValueWrapper existingValue = cache.get(cacheKey);
                    isHit = (existingValue != null);
                }
                
                // Record metric
                if (isHit) {
                    getHitCounter(cacheName).increment();
                } else {
                    getMissCounter(cacheName).increment();
                    getPutCounter(cacheName).increment(); // A miss means a put will happen
                }
                
                // Execute method (cache will handle the actual caching)
                return joinPoint.proceed();
            }

            return joinPoint.proceed();
        }

        @Around("@annotation(org.springframework.cache.annotation.CacheEvict)")
        public Object aroundCacheEvict(ProceedingJoinPoint joinPoint) throws Throwable {
            MethodSignature signature = (MethodSignature) joinPoint.getSignature();
            Method method = signature.getMethod();
            CacheEvict cacheEvict = method.getAnnotation(CacheEvict.class);

            if (cacheEvict != null && cacheEvict.value().length > 0) {
                for (String cacheName : cacheEvict.value()) {
                    getEvictionCounter(cacheName).increment();
                }
            }

            return joinPoint.proceed();
        }

        private String generateCacheKey(ProceedingJoinPoint joinPoint) {
            // Generate a simple cache key from method name and arguments
            StringBuilder key = new StringBuilder(joinPoint.getSignature().getName());
            Object[] args = joinPoint.getArgs();
            if (args != null && args.length > 0) {
                for (Object arg : args) {
                    key.append(":").append(arg != null ? arg.toString() : "null");
                }
            }
            return key.toString();
        }

        private Counter getHitCounter(String cacheName) {
            return hitCounters.computeIfAbsent(cacheName, name -> 
                Counter.builder("cache_gets")
                    .tag("cache", name)
                    .tag("result", "hit")
                    .tag("cacheManager", "cacheManager")
                    .description("Number of cache hits")
                    .register(meterRegistry)
            );
        }

        private Counter getMissCounter(String cacheName) {
            return missCounters.computeIfAbsent(cacheName, name -> 
                Counter.builder("cache_gets")
                    .tag("cache", name)
                    .tag("result", "miss")
                    .tag("cacheManager", "cacheManager")
                    .description("Number of cache misses")
                    .register(meterRegistry)
            );
        }

        private Counter getPutCounter(String cacheName) {
            return putCounters.computeIfAbsent(cacheName, name -> 
                Counter.builder("cache_puts")
                    .tag("cache", name)
                    .tag("cacheManager", "cacheManager")
                    .description("Number of cache puts")
                    .register(meterRegistry)
            );
        }

        private Counter getEvictionCounter(String cacheName) {
            return evictionCounters.computeIfAbsent(cacheName, name ->
                Counter.builder("cache_evictions")
                    .tag("cache", name)
                    .tag("cacheManager", "cacheManager")
                    .description("Number of cache evictions")
                    .register(meterRegistry)
            );
        }
    }
}
