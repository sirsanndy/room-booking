package handler

import (
	"encoding/json"
	"net/http"

	"meetingroom/internal/logger"
	"meetingroom/internal/service"
)

type AuthHandler struct {
	authService *service.AuthService
	log         *logger.Logger
}

func NewAuthHandler(authService *service.AuthService, log *logger.Logger) *AuthHandler {
	return &AuthHandler{
		authService: authService,
		log:         log,
	}
}

type SignupRequest struct {
	Username string `json:"username"`
	Email    string `json:"email"`
	Password string `json:"password"`
}

type LoginRequest struct {
	Username string `json:"username"`
	Password string `json:"password"`
}

type AuthResponse struct {
	Token string      `json:"token"`
	User  interface{} `json:"user"`
}

type ErrorResponse struct {
	Error string `json:"error"`
}

// Signup godoc
// @Summary Register new user
// @Description Register a new user account
// @Tags Authentication
// @Accept json
// @Produce json
// @Param request body SignupRequest true "Signup request"
// @Success 201 {object} AuthResponse "User registered successfully"
// @Failure 400 {object} ErrorResponse "Bad request - Username or email already exists"
// @Router /auth/signup [post]
func (h *AuthHandler) Signup(w http.ResponseWriter, r *http.Request) {
	if r.Method != http.MethodPost {
		h.respondError(w, http.StatusMethodNotAllowed, "Method not allowed")
		return
	}

	var req SignupRequest
	if err := json.NewDecoder(r.Body).Decode(&req); err != nil {
		h.log.Warn("Invalid signup request body", map[string]interface{}{
			"error": err.Error(),
		})
		h.respondError(w, http.StatusBadRequest, "Invalid request body")
		return
	}

	// Validate required fields
	if req.Username == "" || req.Email == "" || req.Password == "" {
		h.respondError(w, http.StatusBadRequest, "Username, email, and password are required")
		return
	}

	// Register user
	user, token, err := h.authService.Register(r.Context(), req.Username, req.Email, req.Password)
	if err != nil {
		h.log.Warn("Signup failed", map[string]interface{}{
			"username": req.Username,
			"error":    err.Error(),
		})

		// Check for specific errors
		errMsg := err.Error()
		if errMsg == "username already exists" || errMsg == "email already exists" {
			h.respondError(w, http.StatusConflict, errMsg)
			return
		}

		h.respondError(w, http.StatusBadRequest, err.Error())
		return
	}

	// Success response
	response := AuthResponse{
		Token: token,
		User:  user,
	}

	h.respondJSON(w, http.StatusCreated, response)
}

// Login godoc
// @Summary User login
// @Description Authenticate user and get JWT token
// @Tags Authentication
// @Accept json
// @Produce json
// @Param request body LoginRequest true "Login request"
// @Success 200 {object} AuthResponse "Login successful"
// @Failure 400 {object} ErrorResponse "Invalid credentials"
// @Router /auth/signin [post]
func (h *AuthHandler) Login(w http.ResponseWriter, r *http.Request) {
	if r.Method != http.MethodPost {
		h.respondError(w, http.StatusMethodNotAllowed, "Method not allowed")
		return
	}

	var req LoginRequest
	if err := json.NewDecoder(r.Body).Decode(&req); err != nil {
		h.log.Warn("Invalid login request body", map[string]interface{}{
			"error": err.Error(),
		})
		h.respondError(w, http.StatusBadRequest, "Invalid request body")
		return
	}

	// Validate required fields
	if req.Username == "" || req.Password == "" {
		h.respondError(w, http.StatusBadRequest, "Username and password are required")
		return
	}

	// Authenticate user
	user, token, err := h.authService.Login(r.Context(), req.Username, req.Password)
	if err != nil {
		h.log.Warn("Login failed", map[string]interface{}{
			"username": req.Username,
			"error":    err.Error(),
		})
		h.respondError(w, http.StatusUnauthorized, "Invalid credentials")
		return
	}

	// Success response
	response := AuthResponse{
		Token: token,
		User:  user,
	}

	h.respondJSON(w, http.StatusOK, response)
}

// Helper methods for JSON responses
func (h *AuthHandler) respondJSON(w http.ResponseWriter, status int, data interface{}) {
	w.Header().Set("Content-Type", "application/json")
	w.WriteHeader(status)
	json.NewEncoder(w).Encode(data)
}

func (h *AuthHandler) respondError(w http.ResponseWriter, status int, message string) {
	w.Header().Set("Content-Type", "application/json")
	w.WriteHeader(status)
	json.NewEncoder(w).Encode(ErrorResponse{Error: message})
}
