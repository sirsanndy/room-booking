package handler

import (
	"encoding/json"
	"net/http"
	"strconv"
	"strings"

	"meetingroom/internal/domain/models"
	"meetingroom/internal/logger"
	"meetingroom/internal/middleware"
	"meetingroom/internal/service"
)

type RoomHandler struct {
	roomService *service.RoomService
	log         *logger.Logger
}

func NewRoomHandler(roomService *service.RoomService, log *logger.Logger) *RoomHandler {
	return &RoomHandler{
		roomService: roomService,
		log:         log,
	}
}

type CreateRoomRequest struct {
	Name      string `json:"name"`
	Capacity  int    `json:"capacity"`
	Location  string `json:"location"`
	Available bool   `json:"available"`
	Features  string `json:"features"`
}

// ListRooms godoc
// @Summary Get all rooms
// @Description Retrieve all meeting rooms or filter by availability
// @Tags Meeting Rooms
// @Produce json
// @Param available query bool false "Filter by availability"
// @Success 200 {array} models.MeetingRoom "Rooms retrieved successfully"
// @Security BearerAuth
// @Router /rooms [get]
func (h *RoomHandler) ListRooms(w http.ResponseWriter, r *http.Request) {
	if r.Method != http.MethodGet {
		h.respondError(w, http.StatusMethodNotAllowed, "Method not allowed")
		return
	}

	// Check if filtering by availability
	availableOnly := r.URL.Query().Get("available") == "true"

	var rooms []*models.MeetingRoom
	var err error

	if availableOnly {
		rooms, err = h.roomService.GetAvailable(r.Context())
	} else {
		rooms, err = h.roomService.GetAll(r.Context())
	}

	if err != nil {
		h.log.Error("Failed to list rooms", map[string]interface{}{
			"error": err.Error(),
		})
		h.respondError(w, http.StatusInternalServerError, "Failed to get rooms")
		return
	}

	h.respondJSON(w, http.StatusOK, rooms)
}

// GetRoom godoc
// @Summary Get room by ID
// @Description Retrieve a specific meeting room by ID
// @Tags Meeting Rooms
// @Produce json
// @Param id path int true "Room ID"
// @Success 200 {object} models.MeetingRoom "Room found"
// @Failure 404 {object} ErrorResponse "Room not found"
// @Security BearerAuth
// @Router /rooms/{id} [get]
func (h *RoomHandler) HandleRoom(w http.ResponseWriter, r *http.Request) {
	// Extract ID from path
	id, err := h.extractIDFromPath(r.URL.Path, "/api/rooms/")
	if err != nil {
		h.respondError(w, http.StatusBadRequest, "Invalid room ID")
		return
	}

	switch r.Method {
	case http.MethodGet:
		h.getRoom(w, r, id)
	case http.MethodPut:
		h.updateRoom(w, r, id)
	case http.MethodDelete:
		h.deleteRoom(w, r, id)
	default:
		h.respondError(w, http.StatusMethodNotAllowed, "Method not allowed")
	}
}

// CreateRoom godoc
// @Summary Create room
// @Description Create a new meeting room (Admin only)
// @Tags Meeting Rooms
// @Accept json
// @Produce json
// @Param request body CreateRoomRequest true "Room details"
// @Success 201 {object} models.MeetingRoom "Room created successfully"
// @Failure 400 {object} ErrorResponse "Invalid request"
// @Failure 403 {object} ErrorResponse "Admin access required"
// @Security BearerAuth
// @Router /rooms [post]
func (h *RoomHandler) CreateRoom(w http.ResponseWriter, r *http.Request) {
	if r.Method != http.MethodPost {
		h.respondError(w, http.StatusMethodNotAllowed, "Method not allowed")
		return
	}

	// Check if user is admin
	user := r.Context().Value(middleware.UserContextKey).(*models.User)
	if !strings.Contains(user.Roles, "ROLE_ADMIN") {
		h.respondError(w, http.StatusForbidden, "Admin access required")
		return
	}

	var req CreateRoomRequest
	if err := json.NewDecoder(r.Body).Decode(&req); err != nil {
		h.respondError(w, http.StatusBadRequest, "Invalid request body")
		return
	}

	room := &models.MeetingRoom{
		Name:      req.Name,
		Capacity:  req.Capacity,
		Location:  req.Location,
		Available: req.Available,
		Features:  req.Features,
	}

	createdRoom, err := h.roomService.Create(r.Context(), room)
	if err != nil {
		h.log.Error("Failed to create room", map[string]interface{}{
			"error": err.Error(),
		})
		h.respondError(w, http.StatusBadRequest, err.Error())
		return
	}

	h.respondJSON(w, http.StatusCreated, createdRoom)
}

func (h *RoomHandler) getRoom(w http.ResponseWriter, r *http.Request, id int64) {
	room, err := h.roomService.GetByID(r.Context(), id)
	if err != nil {
		h.log.Error("Failed to get room", map[string]interface{}{
			"roomId": id,
			"error":  err.Error(),
		})
		h.respondError(w, http.StatusNotFound, "Room not found")
		return
	}

	h.respondJSON(w, http.StatusOK, room)
}

func (h *RoomHandler) updateRoom(w http.ResponseWriter, r *http.Request, id int64) {
	// Check if user is admin
	user := r.Context().Value(middleware.UserContextKey).(*models.User)
	if !strings.Contains(user.Roles, "ROLE_ADMIN") {
		h.respondError(w, http.StatusForbidden, "Admin access required")
		return
	}

	var req CreateRoomRequest
	if err := json.NewDecoder(r.Body).Decode(&req); err != nil {
		h.respondError(w, http.StatusBadRequest, "Invalid request body")
		return
	}

	room := &models.MeetingRoom{
		Name:      req.Name,
		Capacity:  req.Capacity,
		Location:  req.Location,
		Available: req.Available,
		Features:  req.Features,
	}

	updatedRoom, err := h.roomService.Update(r.Context(), id, room)
	if err != nil {
		h.log.Error("Failed to update room", map[string]interface{}{
			"roomId": id,
			"error":  err.Error(),
		})
		h.respondError(w, http.StatusBadRequest, err.Error())
		return
	}

	h.respondJSON(w, http.StatusOK, updatedRoom)
}

func (h *RoomHandler) deleteRoom(w http.ResponseWriter, r *http.Request, id int64) {
	// Check if user is admin
	user := r.Context().Value(middleware.UserContextKey).(*models.User)
	if !strings.Contains(user.Roles, "ROLE_ADMIN") {
		h.respondError(w, http.StatusForbidden, "Admin access required")
		return
	}

	if err := h.roomService.Delete(r.Context(), id); err != nil {
		h.log.Error("Failed to delete room", map[string]interface{}{
			"roomId": id,
			"error":  err.Error(),
		})
		h.respondError(w, http.StatusBadRequest, err.Error())
		return
	}

	h.respondJSON(w, http.StatusOK, map[string]string{"message": "Room deleted successfully"})
}

// Helper methods
func (h *RoomHandler) extractIDFromPath(path, prefix string) (int64, error) {
	idStr := strings.TrimPrefix(path, prefix)
	return strconv.ParseInt(idStr, 10, 64)
}

func (h *RoomHandler) respondJSON(w http.ResponseWriter, status int, data interface{}) {
	w.Header().Set("Content-Type", "application/json")
	w.WriteHeader(status)
	json.NewEncoder(w).Encode(data)
}

func (h *RoomHandler) respondError(w http.ResponseWriter, status int, message string) {
	w.Header().Set("Content-Type", "application/json")
	w.WriteHeader(status)
	json.NewEncoder(w).Encode(map[string]string{"error": message})
}
