package handler

import (
	"encoding/json"
	"net/http"
	"strconv"
	"strings"
	"time"

	"meetingroom/internal/domain/models"
	"meetingroom/internal/logger"
	"meetingroom/internal/middleware"
	"meetingroom/internal/service"
)

type BookingHandler struct {
	bookingService *service.BookingService
	log            *logger.Logger
}

func NewBookingHandler(bookingService *service.BookingService, log *logger.Logger) *BookingHandler {
	return &BookingHandler{
		bookingService: bookingService,
		log:            log,
	}
}

type CreateBookingRequest struct {
	RoomID      int64  `json:"roomId"`
	StartTime   string `json:"startTime"`
	EndTime     string `json:"endTime"`
	Title       string `json:"title"`
	Description string `json:"description"`
}

// CreateBooking godoc
// @Summary Create booking
// @Description Create a new meeting room booking
// @Tags Bookings
// @Accept json
// @Produce json
// @Param request body CreateBookingRequest true "Booking details"
// @Success 201 {object} models.Booking "Booking created successfully"
// @Failure 400 {object} ErrorResponse "Invalid booking request"
// @Failure 409 {object} ErrorResponse "Booking conflict"
// @Security BearerAuth
// @Router /bookings [post]
func (h *BookingHandler) HandleBookings(w http.ResponseWriter, r *http.Request) {
	switch r.Method {
	case http.MethodGet:
		h.listBookings(w, r)
	case http.MethodPost:
		h.createBooking(w, r)
	default:
		h.respondError(w, http.StatusMethodNotAllowed, "Method not allowed")
	}
}

// HandleBooking handles GET/PUT/DELETE /api/bookings/{id}
func (h *BookingHandler) HandleBooking(w http.ResponseWriter, r *http.Request) {
	// Extract ID from path
	id, err := h.extractIDFromPath(r.URL.Path, "/api/bookings/")
	if err != nil {
		h.respondError(w, http.StatusBadRequest, "Invalid booking ID")
		return
	}

	switch r.Method {
	case http.MethodGet:
		h.getBooking(w, r, id)
	case http.MethodPut:
		h.updateBooking(w, r, id)
	case http.MethodDelete:
		h.cancelBooking(w, r, id)
	default:
		h.respondError(w, http.StatusMethodNotAllowed, "Method not allowed")
	}
}

// GetMyBookings godoc
// @Summary Get my bookings
// @Description Get all bookings for the authenticated user
// @Tags Bookings
// @Produce json
// @Success 200 {array} models.BookingWithDetails "Bookings retrieved successfully"
// @Security BearerAuth
// @Router /bookings/my-bookings [get]
func (h *BookingHandler) GetMyBookings(w http.ResponseWriter, r *http.Request) {
	if r.Method != http.MethodGet {
		h.respondError(w, http.StatusMethodNotAllowed, "Method not allowed")
		return
	}

	user := r.Context().Value(middleware.UserContextKey).(*models.User)

	bookings, err := h.bookingService.GetMyBookings(r.Context(), user.ID)
	if err != nil {
		h.log.Error("Failed to get user bookings", map[string]interface{}{
			"userId": user.ID,
			"error":  err.Error(),
		})
		h.respondError(w, http.StatusInternalServerError, "Failed to get bookings")
		return
	}

	h.respondJSON(w, http.StatusOK, bookings)
}

// GetBookingsByRoom godoc
// @Summary Get bookings by room
// @Description Get upcoming bookings for a specific room
// @Tags Bookings
// @Produce json
// @Param id path int true "Room ID"
// @Param start query string true "Start time (RFC3339)"
// @Param end query string true "End time (RFC3339)"
// @Success 200 {array} models.BookingWithDetails "Bookings retrieved successfully"
// @Security BearerAuth
// @Router /bookings/room/{id} [get]
func (h *BookingHandler) GetBookingsByRoom(w http.ResponseWriter, r *http.Request) {
	if r.Method != http.MethodGet {
		h.respondError(w, http.StatusMethodNotAllowed, "Method not allowed")
		return
	}

	// Extract room ID from path
	roomID, err := h.extractIDFromPath(r.URL.Path, "/api/bookings/room/")
	if err != nil {
		h.respondError(w, http.StatusBadRequest, "Invalid room ID")
		return
	}

	// Parse query parameters for time range
	startStr := r.URL.Query().Get("start")
	endStr := r.URL.Query().Get("end")

	if startStr == "" || endStr == "" {
		h.respondError(w, http.StatusBadRequest, "Start and end time are required")
		return
	}

	start, err := time.Parse(time.RFC3339, startStr)
	if err != nil {
		h.respondError(w, http.StatusBadRequest, "Invalid start time format")
		return
	}

	end, err := time.Parse(time.RFC3339, endStr)
	if err != nil {
		h.respondError(w, http.StatusBadRequest, "Invalid end time format")
		return
	}

	bookings, err := h.bookingService.GetByRoomID(r.Context(), roomID, start, end)
	if err != nil {
		h.log.Error("Failed to get room bookings", map[string]interface{}{
			"roomId": roomID,
			"error":  err.Error(),
		})
		h.respondError(w, http.StatusInternalServerError, "Failed to get bookings")
		return
	}

	h.respondJSON(w, http.StatusOK, bookings)
}

func (h *BookingHandler) listBookings(w http.ResponseWriter, r *http.Request) {
	// For simplicity, return user's bookings
	user := r.Context().Value(middleware.UserContextKey).(*models.User)

	bookings, err := h.bookingService.GetMyBookings(r.Context(), user.ID)
	if err != nil {
		h.log.Error("Failed to list bookings", map[string]interface{}{
			"error": err.Error(),
		})
		h.respondError(w, http.StatusInternalServerError, "Failed to get bookings")
		return
	}

	h.respondJSON(w, http.StatusOK, bookings)
}

func (h *BookingHandler) createBooking(w http.ResponseWriter, r *http.Request) {
	user := r.Context().Value(middleware.UserContextKey).(*models.User)

	var req CreateBookingRequest
	if err := json.NewDecoder(r.Body).Decode(&req); err != nil {
		h.respondError(w, http.StatusBadRequest, "Invalid request body")
		return
	}

	// Parse times
	startTime, err := time.Parse(time.RFC3339, req.StartTime)
	if err != nil {
		h.respondError(w, http.StatusBadRequest, "Invalid start time format")
		return
	}

	endTime, err := time.Parse(time.RFC3339, req.EndTime)
	if err != nil {
		h.respondError(w, http.StatusBadRequest, "Invalid end time format")
		return
	}

	booking := &models.Booking{
		RoomID:      req.RoomID,
		StartTime:   startTime,
		EndTime:     endTime,
		Title:       req.Title,
		Description: req.Description,
	}

	createdBooking, err := h.bookingService.Create(r.Context(), user.ID, booking)
	if err != nil {
		h.log.Error("Failed to create booking", map[string]interface{}{
			"userId": user.ID,
			"error":  err.Error(),
		})
		h.respondError(w, http.StatusBadRequest, err.Error())
		return
	}

	h.respondJSON(w, http.StatusCreated, createdBooking)
}

func (h *BookingHandler) getBooking(w http.ResponseWriter, r *http.Request, id int64) {
	booking, err := h.bookingService.GetByID(r.Context(), id)
	if err != nil {
		h.log.Error("Failed to get booking", map[string]interface{}{
			"bookingId": id,
			"error":     err.Error(),
		})
		h.respondError(w, http.StatusNotFound, "Booking not found")
		return
	}

	h.respondJSON(w, http.StatusOK, booking)
}

func (h *BookingHandler) updateBooking(w http.ResponseWriter, r *http.Request, id int64) {
	user := r.Context().Value(middleware.UserContextKey).(*models.User)

	var req CreateBookingRequest
	if err := json.NewDecoder(r.Body).Decode(&req); err != nil {
		h.respondError(w, http.StatusBadRequest, "Invalid request body")
		return
	}

	// Parse times
	startTime, err := time.Parse(time.RFC3339, req.StartTime)
	if err != nil {
		h.respondError(w, http.StatusBadRequest, "Invalid start time format")
		return
	}

	endTime, err := time.Parse(time.RFC3339, req.EndTime)
	if err != nil {
		h.respondError(w, http.StatusBadRequest, "Invalid end time format")
		return
	}

	booking := &models.Booking{
		RoomID:      req.RoomID,
		StartTime:   startTime,
		EndTime:     endTime,
		Title:       req.Title,
		Description: req.Description,
	}

	updatedBooking, err := h.bookingService.Update(r.Context(), id, user.ID, booking)
	if err != nil {
		h.log.Error("Failed to update booking", map[string]interface{}{
			"bookingId": id,
			"error":     err.Error(),
		})
		h.respondError(w, http.StatusBadRequest, err.Error())
		return
	}

	h.respondJSON(w, http.StatusOK, updatedBooking)
}

// CancelBooking godoc
// @Summary Cancel booking
// @Description Cancel a booking by ID
// @Tags Bookings
// @Produce json
// @Param id path int true "Booking ID"
// @Param version query int false "Booking version for optimistic locking"
// @Success 200 {object} map[string]string "Booking cancelled successfully"
// @Failure 400 {object} ErrorResponse "Cannot cancel booking"
// @Security BearerAuth
// @Router /bookings/{id} [delete]
func (h *BookingHandler) cancelBooking(w http.ResponseWriter, r *http.Request, id int64) {
	user := r.Context().Value(middleware.UserContextKey).(*models.User)

	// Get version from query parameter
	versionStr := r.URL.Query().Get("version")
	version := 0
	if versionStr != "" {
		v, err := strconv.Atoi(versionStr)
		if err == nil {
			version = v
		}
	}

	if err := h.bookingService.Cancel(r.Context(), id, user.ID, version); err != nil {
		h.log.Error("Failed to cancel booking", map[string]interface{}{
			"bookingId": id,
			"error":     err.Error(),
		})
		h.respondError(w, http.StatusBadRequest, err.Error())
		return
	}

	h.respondJSON(w, http.StatusOK, map[string]string{"message": "Booking cancelled successfully"})
}

// Helper methods
func (h *BookingHandler) extractIDFromPath(path, prefix string) (int64, error) {
	idStr := strings.TrimPrefix(path, prefix)
	// Remove any trailing parts (e.g., /api/bookings/123/cancel -> 123)
	parts := strings.Split(idStr, "/")
	return strconv.ParseInt(parts[0], 10, 64)
}

func (h *BookingHandler) respondJSON(w http.ResponseWriter, status int, data interface{}) {
	w.Header().Set("Content-Type", "application/json")
	w.WriteHeader(status)
	json.NewEncoder(w).Encode(data)
}

func (h *BookingHandler) respondError(w http.ResponseWriter, status int, message string) {
	w.Header().Set("Content-Type", "application/json")
	w.WriteHeader(status)
	json.NewEncoder(w).Encode(map[string]string{"error": message})
}
