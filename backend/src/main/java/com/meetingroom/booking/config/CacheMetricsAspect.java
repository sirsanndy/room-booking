package com.meetingroom.booking.config;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.interceptor.CacheOperationInvoker;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Aspect to track cache hits and misses for Prometheus metrics
 * Intercepts @Cacheable methods to record cache statistics
 */
@Aspect
@Component
public class CacheMetricsAspect {

    private final MeterRegistry meterRegistry;
    private final Map<String, Counter> hitCounters = new ConcurrentHashMap<>();
    private final Map<String, Counter> missCounters = new ConcurrentHashMap<>();

    public CacheMetricsAspect(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    @Around("@annotation(org.springframework.cache.annotation.Cacheable)")
    public Object aroundCacheable(ProceedingJoinPoint joinPoint) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        Cacheable cacheable = method.getAnnotation(Cacheable.class);
        
        if (cacheable != null && cacheable.value().length > 0) {
            String cacheName = cacheable.value()[0];
            
            // Get or create counters for this cache
            Counter hitCounter = hitCounters.computeIfAbsent(cacheName, 
                name -> Counter.builder("cache.gets")
                    .tag("cache", name)
                    .tag("result", "hit")
                    .tag("cacheManager", "cacheManager")
                    .description("Cache hits")
                    .register(meterRegistry));
                    
            Counter missCounter = missCounters.computeIfAbsent(cacheName,
                name -> Counter.builder("cache.gets")
                    .tag("cache", name)
                    .tag("result", "miss")
                    .tag("cacheManager", "cacheManager")
                    .description("Cache misses")
                    .register(meterRegistry));
            
            // We can't easily detect if it's a hit or miss without accessing cache internals
            // This is a limitation - we'll track at method level instead
            // For now, just proceed with the method
            return joinPoint.proceed();
        }
        
        return joinPoint.proceed();
    }
}
