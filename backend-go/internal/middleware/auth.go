package middleware

import (
	"context"
	"net/http"

	"meetingroom/internal/config"
	"meetingroom/internal/logger"
	"meetingroom/internal/repository"
	"meetingroom/internal/security"
)

type contextKey string

const (
	UserContextKey contextKey = "user"
)

// JWTAuth middleware validates JWT tokens and adds user to context
func JWTAuth(cfg config.JWTConfig, userRepo *repository.UserRepository, log *logger.Logger) Middleware {
	return func(next http.Handler) http.Handler {
		return http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
			// Extract token from Authorization header
			authHeader := r.Header.Get("Authorization")
			if authHeader == "" {
				log.Warn("Missing authorization header", map[string]interface{}{
					"path": r.URL.Path,
				})
				http.Error(w, `{"error":"Missing authorization token"}`, http.StatusUnauthorized)
				return
			}

			tokenString, err := security.ExtractToken(authHeader)
			if err != nil {
				log.Warn("Invalid authorization header", map[string]interface{}{
					"error": err.Error(),
					"path":  r.URL.Path,
				})
				http.Error(w, `{"error":"Invalid authorization header"}`, http.StatusUnauthorized)
				return
			}

			// Validate token
			claims, err := security.ValidateJWT(tokenString, cfg)
			if err != nil {
				log.Warn("Invalid JWT token", map[string]interface{}{
					"error": err.Error(),
					"path":  r.URL.Path,
				})
				http.Error(w, `{"error":"Invalid or expired token"}`, http.StatusUnauthorized)
				return
			}

			// Get user from database (with caching)
			user, err := userRepo.FindByID(r.Context(), claims.UserID)
			if err != nil {
				log.Error("Failed to fetch user", map[string]interface{}{
					"error":  err.Error(),
					"userId": claims.UserID,
				})
				http.Error(w, `{"error":"User not found"}`, http.StatusUnauthorized)
				return
			}

			if !user.Enabled {
				log.Warn("User account disabled", map[string]interface{}{
					"userId": claims.UserID,
				})
				http.Error(w, `{"error":"User account is disabled"}`, http.StatusUnauthorized)
				return
			}

			// Add user to context
			ctx := context.WithValue(r.Context(), UserContextKey, user)
			next.ServeHTTP(w, r.WithContext(ctx))
		})
	}
}
