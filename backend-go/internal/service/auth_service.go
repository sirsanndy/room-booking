package service

import (
	"context"
	"fmt"
	"strings"

	"meetingroom/internal/config"
	"meetingroom/internal/domain/models"
	"meetingroom/internal/logger"
	"meetingroom/internal/repository"
	"meetingroom/internal/security"
)

type AuthService struct {
	userRepo *repository.UserRepository
	jwtCfg   config.JWTConfig
	log      *logger.Logger
}

func NewAuthService(userRepo *repository.UserRepository, jwtCfg config.JWTConfig, log *logger.Logger) *AuthService {
	return &AuthService{
		userRepo: userRepo,
		jwtCfg:   jwtCfg,
		log:      log,
	}
}

// Register creates a new user account
func (s *AuthService) Register(ctx context.Context, username, email, password string) (*models.User, string, error) {
	// Validate input
	if err := s.validateRegistrationInput(username, email, password); err != nil {
		return nil, "", err
	}

	// Check if username already exists
	existingUser, _ := s.userRepo.FindByUsername(ctx, username)
	if existingUser != nil {
		return nil, "", fmt.Errorf("username already exists")
	}

	// Check if email already exists
	existingUser, _ = s.userRepo.FindByEmail(ctx, email)
	if existingUser != nil {
		return nil, "", fmt.Errorf("email already exists")
	}

	// Hash password (already SHA-256 from frontend, now applying BCrypt)
	hashedPassword, err := security.HashPassword(password)
	if err != nil {
		s.log.Error("Failed to hash password", map[string]interface{}{
			"error": err.Error(),
		})
		return nil, "", fmt.Errorf("failed to hash password")
	}

	// Create user
	user := &models.User{
		Username: username,
		Email:    email,
		Password: hashedPassword,
		Roles:    "ROLE_USER",
		Enabled:  true,
	}

	if err := s.userRepo.Create(ctx, user); err != nil {
		s.log.Error("Failed to create user", map[string]interface{}{
			"username": username,
			"error":    err.Error(),
		})
		return nil, "", fmt.Errorf("failed to create user: %w", err)
	}

	// Generate JWT token
	token, err := security.GenerateJWT(user.ID, user.Username, user.Roles, s.jwtCfg)
	if err != nil {
		s.log.Error("Failed to generate JWT", map[string]interface{}{
			"userId": user.ID,
			"error":  err.Error(),
		})
		return nil, "", fmt.Errorf("failed to generate token")
	}

	// Don't return password
	user.Password = ""

	s.log.Info("User registered successfully", map[string]interface{}{
		"userId":   user.ID,
		"username": user.Username,
	})

	return user, token, nil
}

// Login authenticates a user and returns a JWT token
func (s *AuthService) Login(ctx context.Context, username, password string) (*models.User, string, error) {
	// Validate input
	if username == "" || password == "" {
		return nil, "", fmt.Errorf("username and password are required")
	}

	// Find user
	user, err := s.userRepo.FindByUsername(ctx, username)
	if err != nil {
		s.log.Warn("Login attempt with invalid username", map[string]interface{}{
			"username": username,
		})
		return nil, "", fmt.Errorf("invalid credentials")
	}

	// Check if user is enabled
	if !user.Enabled {
		s.log.Warn("Login attempt for disabled user", map[string]interface{}{
			"userId":   user.ID,
			"username": user.Username,
		})
		return nil, "", fmt.Errorf("account is disabled")
	}

	// Verify password
	if !security.VerifyPassword(password, user.Password) {
		s.log.Warn("Login attempt with invalid password", map[string]interface{}{
			"userId":   user.ID,
			"username": user.Username,
		})
		return nil, "", fmt.Errorf("invalid credentials")
	}

	// Generate JWT token
	token, err := security.GenerateJWT(user.ID, user.Username, user.Roles, s.jwtCfg)
	if err != nil {
		s.log.Error("Failed to generate JWT", map[string]interface{}{
			"userId": user.ID,
			"error":  err.Error(),
		})
		return nil, "", fmt.Errorf("failed to generate token")
	}

	// Don't return password
	user.Password = ""

	s.log.Info("User logged in successfully", map[string]interface{}{
		"userId":   user.ID,
		"username": user.Username,
	})

	return user, token, nil
}

// validateRegistrationInput validates registration input
func (s *AuthService) validateRegistrationInput(username, email, password string) error {
	if username == "" {
		return fmt.Errorf("username is required")
	}
	if email == "" {
		return fmt.Errorf("email is required")
	}
	if password == "" {
		return fmt.Errorf("password is required")
	}

	// Username validation
	if len(username) < 3 || len(username) > 50 {
		return fmt.Errorf("username must be between 3 and 50 characters")
	}

	// Email validation (basic)
	if !strings.Contains(email, "@") || !strings.Contains(email, ".") {
		return fmt.Errorf("invalid email format")
	}

	// Password validation (already SHA-256 hashed from frontend, so 64 chars)
	if len(password) != 64 && len(password) < 8 {
		return fmt.Errorf("password must be at least 8 characters")
	}

	return nil
}
