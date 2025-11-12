package middleware

import (
	"net/http"
	"sync"
	"time"

	"meetingroom/internal/config"

	"golang.org/x/time/rate"
)

// RateLimit middleware implements token bucket rate limiting
func RateLimit(cfg config.RateLimitConfig) Middleware {
	limiters := &sync.Map{}

	return func(next http.Handler) http.Handler {
		return http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
			// Get client IP
			ip := r.RemoteAddr

			// Get or create rate limiter for this IP
			limiterInterface, _ := limiters.LoadOrStore(ip, rate.NewLimiter(
				rate.Limit(cfg.RequestsPerSecond),
				cfg.Burst,
			))
			limiter := limiterInterface.(*rate.Limiter)

			// Check if request is allowed
			if !limiter.Allow() {
				w.Header().Set("Content-Type", "application/json")
				w.Header().Set("Retry-After", "1")
				w.WriteHeader(http.StatusTooManyRequests)
				w.Write([]byte(`{"error":"Too many requests. Please try again later."}`))
				return
			}

			next.ServeHTTP(w, r)
		})
	}
}

// CleanupRateLimiters periodically cleans up old rate limiters
func CleanupRateLimiters(limiters *sync.Map, interval time.Duration) {
	ticker := time.NewTicker(interval)
	defer ticker.Stop()

	for range ticker.C {
		// TODO: Implement cleanup logic for inactive limiters
		// This would require tracking last access time
	}
}
