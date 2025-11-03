package com.meetingroom.booking.config;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

/**
 * Custom metrics configuration for detailed endpoint monitoring
 * Tracks HTTP status codes, response times, and endpoint-specific metrics
 */
@Configuration
public class MetricsConfig {

    /**
     * Register custom HTTP metrics filter
     */
    @Bean
    public FilterRegistrationBean<HttpMetricsFilter> httpMetricsFilter(MeterRegistry meterRegistry) {
        FilterRegistrationBean<HttpMetricsFilter> registrationBean = new FilterRegistrationBean<>();
        registrationBean.setFilter(new HttpMetricsFilter(meterRegistry));
        registrationBean.addUrlPatterns("/api/*");
        registrationBean.setOrder(Ordered.HIGHEST_PRECEDENCE);
        return registrationBean;
    }

    /**
     * Filter to capture HTTP metrics for each request
     */
    public static class HttpMetricsFilter extends OncePerRequestFilter {
        
        private final MeterRegistry meterRegistry;
        
        public HttpMetricsFilter(MeterRegistry meterRegistry) {
            this.meterRegistry = meterRegistry;
        }
        
        @Override
        protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, 
                                        FilterChain filterChain) throws ServletException, IOException {
            
            long startTime = System.nanoTime();
            String method = request.getMethod();
            String uri = getEndpointPath(request.getRequestURI());
            
            try {
                filterChain.doFilter(request, response);
            } finally {
                long duration = System.nanoTime() - startTime;
                int status = response.getStatus();
                
                // Record metrics
                recordHttpMetrics(method, uri, status, duration);
            }
        }
        
        /**
         * Extract endpoint path (normalize path parameters)
         */
        private String getEndpointPath(String uri) {
            // Remove /api prefix for cleaner metrics
            String path = uri.replaceFirst("^/api", "");
            
            // Normalize paths with IDs (e.g., /bookings/123 -> /bookings/{id})
            path = path.replaceAll("/\\d+$", "/{id}");
            path = path.replaceAll("/\\d+/", "/{id}/");
            
            // Handle empty path
            if (path.isEmpty()) {
                path = "/";
            }
            
            return path;
        }
        
        /**
         * Record HTTP metrics for the request
         */
        private void recordHttpMetrics(String method, String uri, int status, long durationNanos) {
            String statusCategory = getStatusCategory(status);
            String statusCode = String.valueOf(status);
            
            // 1. Total requests counter
            Counter.builder("http_requests_total")
                    .description("Total HTTP requests")
                    .tag("method", method)
                    .tag("endpoint", uri)
                    .tag("status", statusCode)
                    .tag("status_category", statusCategory)
                    .register(meterRegistry)
                    .increment();
            
            // 2. Status-specific counters
            Counter.builder("http_requests_" + statusCategory)
                    .description("HTTP requests by status category")
                    .tag("method", method)
                    .tag("endpoint", uri)
                    .tag("status", statusCode)
                    .register(meterRegistry)
                    .increment();
            
            // 3. Response time distribution
            Timer.builder("http_request_duration_seconds")
                    .description("HTTP request duration in seconds")
                    .tag("method", method)
                    .tag("endpoint", uri)
                    .tag("status", statusCode)
                    .tag("status_category", statusCategory)
                    .register(meterRegistry)
                    .record(durationNanos, TimeUnit.NANOSECONDS);
            
            // 4. Endpoint-specific counters
            Counter.builder("endpoint_requests_total")
                    .description("Total requests per endpoint")
                    .tag("endpoint", uri)
                    .tag("method", method)
                    .register(meterRegistry)
                    .increment();
            
            // 5. Error counters (4xx and 5xx)
            if (status >= 400) {
                Counter.builder("http_errors_total")
                        .description("Total HTTP errors")
                        .tag("method", method)
                        .tag("endpoint", uri)
                        .tag("status", statusCode)
                        .tag("error_type", status >= 500 ? "server_error" : "client_error")
                        .register(meterRegistry)
                        .increment();
            }
            
            // 6. Specific status code counters
            recordSpecificStatusMetrics(method, uri, status);
        }
        
        /**
         * Record metrics for specific important status codes
         */
        private void recordSpecificStatusMetrics(String method, String uri, int status) {
            String metricName = null;
            String description = null;
            
            switch (status) {
                case 200:
                    metricName = "http_200_ok";
                    description = "HTTP 200 OK responses";
                    break;
                case 201:
                    metricName = "http_201_created";
                    description = "HTTP 201 Created responses";
                    break;
                case 400:
                    metricName = "http_400_bad_request";
                    description = "HTTP 400 Bad Request errors";
                    break;
                case 401:
                    metricName = "http_401_unauthorized";
                    description = "HTTP 401 Unauthorized errors";
                    break;
                case 403:
                    metricName = "http_403_forbidden";
                    description = "HTTP 403 Forbidden errors";
                    break;
                case 404:
                    metricName = "http_404_not_found";
                    description = "HTTP 404 Not Found errors";
                    break;
                case 429:
                    metricName = "http_429_too_many_requests";
                    description = "HTTP 429 Too Many Requests (Rate Limited)";
                    break;
                case 500:
                    metricName = "http_500_internal_error";
                    description = "HTTP 500 Internal Server Error";
                    break;
                case 503:
                    metricName = "http_503_service_unavailable";
                    description = "HTTP 503 Service Unavailable";
                    break;
            }
            
            if (metricName != null) {
                Counter.builder(metricName)
                        .description(description)
                        .tag("method", method)
                        .tag("endpoint", uri)
                        .register(meterRegistry)
                        .increment();
            }
        }
        
        /**
         * Get status category (success, client_error, server_error)
         */
        private String getStatusCategory(int status) {
            if (status >= 200 && status < 300) {
                return "success";
            } else if (status >= 300 && status < 400) {
                return "redirect";
            } else if (status >= 400 && status < 500) {
                return "client_error";
            } else if (status >= 500) {
                return "server_error";
            } else {
                return "informational";
            }
        }
    }
}
