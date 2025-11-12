package middleware

import (
	"fmt"
	"net/http"
	"runtime/debug"

	"meetingroom/internal/logger"
)

// Recovery middleware recovers from panics and logs the error
func Recovery(log *logger.Logger) Middleware {
	return func(next http.Handler) http.Handler {
		return http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
			defer func() {
				if err := recover(); err != nil {
					// Log panic with stack trace
					log.Error("Panic recovered", map[string]interface{}{
						"error":      fmt.Sprintf("%v", err),
						"path":       r.URL.Path,
						"method":     r.Method,
						"stackTrace": string(debug.Stack()),
					})

					// Return 500 error
					w.Header().Set("Content-Type", "application/json")
					w.WriteHeader(http.StatusInternalServerError)
					w.Write([]byte(`{"error":"Internal server error"}`))
				}
			}()

			next.ServeHTTP(w, r)
		})
	}
}
