package handler

import (
	"encoding/json"
	"net/http"

	"meetingroom/internal/logger"
	"meetingroom/internal/service"
)

type DashboardHandler struct {
	dashboardService *service.DashboardService
	log              *logger.Logger
}

func NewDashboardHandler(dashboardService *service.DashboardService, log *logger.Logger) *DashboardHandler {
	return &DashboardHandler{
		dashboardService: dashboardService,
		log:              log,
	}
}

// GetDashboard godoc
// @Summary Get dashboard data
// @Description Get dashboard data including calendar events and statistics
// @Tags Dashboard
// @Produce json
// @Success 200 {object} models.DashboardStats "Dashboard data retrieved successfully"
// @Security BearerAuth
// @Router /dashboard [get]
func (h *DashboardHandler) GetDashboard(w http.ResponseWriter, r *http.Request) {
	if r.Method != http.MethodGet {
		h.respondError(w, http.StatusMethodNotAllowed, "Method not allowed")
		return
	}

	stats, err := h.dashboardService.GetStats(r.Context())
	if err != nil {
		h.log.Error("Failed to get dashboard stats", map[string]interface{}{
			"error": err.Error(),
		})
		h.respondError(w, http.StatusInternalServerError, "Failed to get dashboard stats")
		return
	}

	h.respondJSON(w, http.StatusOK, stats)
}

// Helper methods
func (h *DashboardHandler) respondJSON(w http.ResponseWriter, status int, data interface{}) {
	w.Header().Set("Content-Type", "application/json")
	w.WriteHeader(status)
	json.NewEncoder(w).Encode(data)
}

func (h *DashboardHandler) respondError(w http.ResponseWriter, status int, message string) {
	w.Header().Set("Content-Type", "application/json")
	w.WriteHeader(status)
	json.NewEncoder(w).Encode(map[string]string{"error": message})
}
