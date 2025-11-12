package middleware

import (
	"net/http"
	"time"

	"meetingroom/internal/metrics"
)

// Metrics middleware records HTTP metrics
func Metrics(collector *metrics.Collector) Middleware {
	return func(next http.Handler) http.Handler {
		return http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
			start := time.Now()
			collector.InFlightHTTPRequest(1)

			// Wrap response writer to capture status code
			wrapped := &metricsResponseWriter{
				ResponseWriter: w,
				statusCode:     http.StatusOK,
			}

			// Call next handler
			next.ServeHTTP(wrapped, r)

			// Record metrics
			duration := time.Since(start)
			collector.InFlightHTTPRequest(-1)
			collector.RecordHTTPRequest(
				r.Method,
				r.URL.Path,
				http.StatusText(wrapped.statusCode),
				duration,
			)
		})
	}
}

type metricsResponseWriter struct {
	http.ResponseWriter
	statusCode int
}

func (mrw *metricsResponseWriter) WriteHeader(code int) {
	mrw.statusCode = code
	mrw.ResponseWriter.WriteHeader(code)
}
