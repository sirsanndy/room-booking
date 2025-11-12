package main

import (
	"context"
	"fmt"
	"log"
	"meetingroom/internal/config"
	"meetingroom/internal/handler"
	"meetingroom/internal/logger"
	"meetingroom/internal/metrics"
	"meetingroom/internal/middleware"
	"meetingroom/internal/repository"
	"meetingroom/internal/service"
	"net/http"
	"os"
	"os/signal"
	"syscall"
	"time"

	_ "meetingroom/docs" // Swagger docs

	"github.com/prometheus/client_golang/prometheus/promhttp"
	httpSwagger "github.com/swaggo/http-swagger/v2"
)

// @title Meeting Room Booking API
// @version 1.0
// @description API for managing meeting room bookings, including authentication, room management, and booking operations.
// @contact.name API Support
// @contact.email support@example.com
// @host localhost:8080
// @BasePath /api
// @securityDefinitions.apikey BearerAuth
// @in header
// @name Authorization
// @description Type "Bearer" followed by a space and JWT token.
func main() {
	// Load configuration
	cfg, err := config.Load()
	if err != nil {
		log.Fatalf("Failed to load configuration: %v", err)
	}

	// Initialize logger
	appLogger, err := logger.NewLogger(cfg.Log)
	if err != nil {
		log.Fatalf("Failed to initialize logger: %v", err)
	}
	defer appLogger.Close()

	appLogger.Info("Starting Meeting Room Booking API", map[string]interface{}{
		"port": cfg.Server.Port,
		"env":  cfg.Server.Env,
	})

	// Initialize database
	db, err := config.NewDatabase(cfg.Database)
	if err != nil {
		appLogger.Fatal("Failed to connect to database", map[string]interface{}{
			"error": err.Error(),
		})
	}
	defer db.Close()

	appLogger.Info("Database connection established", map[string]interface{}{
		"host": cfg.Database.Host,
		"port": cfg.Database.Port,
	})

	// Initialize Redis cache
	cache, err := config.NewRedisCache(cfg.Redis)
	if err != nil {
		appLogger.Fatal("Failed to connect to Redis", map[string]interface{}{
			"error": err.Error(),
		})
	}
	defer cache.Close()

	appLogger.Info("Redis connection established", map[string]interface{}{
		"host": cfg.Redis.Host,
		"port": cfg.Redis.Port,
	})

	// Initialize metrics
	metricsCollector := metrics.NewCollector()
	if cfg.Metrics.Enabled {
		go metricsCollector.StartMetricsServer(cfg.Metrics.Port)
		appLogger.Info("Metrics server started", map[string]interface{}{
			"port": cfg.Metrics.Port,
		})
	}

	// Initialize repositories
	userRepo := repository.NewUserRepository(db, cache, appLogger)
	roomRepo := repository.NewRoomRepository(db, cache, appLogger)
	bookingRepo := repository.NewBookingRepository(db, appLogger)
	holidayRepo := repository.NewHolidayRepository(db, cache, appLogger)

	// Initialize services
	authService := service.NewAuthService(userRepo, cfg.JWT, appLogger)
	roomService := service.NewRoomService(roomRepo, cache, appLogger)
	bookingService := service.NewBookingService(bookingRepo, roomRepo, userRepo, db, appLogger)
	dashboardService := service.NewDashboardService(bookingRepo, roomRepo, holidayRepo, appLogger)

	// Initialize handlers
	authHandler := handler.NewAuthHandler(authService, appLogger)
	roomHandler := handler.NewRoomHandler(roomService, appLogger)
	bookingHandler := handler.NewBookingHandler(bookingService, appLogger)
	dashboardHandler := handler.NewDashboardHandler(dashboardService, appLogger)

	// Setup router
	mux := http.NewServeMux()

	// Initialize middleware chain
	chain := middleware.NewChain(
		middleware.Recovery(appLogger),
		middleware.CORS(cfg.CORS),
		middleware.Logging(appLogger),
		middleware.Metrics(metricsCollector),
		middleware.RateLimit(cfg.RateLimit),
	)

	// Health check endpoint (no auth required)
	mux.HandleFunc("/health", func(w http.ResponseWriter, r *http.Request) {
		w.Header().Set("Content-Type", "application/json")
		w.WriteHeader(http.StatusOK)
		w.Write([]byte(`{"status":"healthy","timestamp":"` + time.Now().Format(time.RFC3339) + `"}`))
	})

	// Swagger documentation endpoint
	mux.HandleFunc("/swagger/", httpSwagger.WrapHandler)

	// Prometheus metrics endpoint (matching Spring Boot Actuator path)
	mux.Handle("/actuator/prometheus", promhttp.Handler())

	// Also keep the standard /metrics endpoint for compatibility
	mux.Handle("/metrics", promhttp.Handler())

	// Auth endpoints (no auth required)
	mux.HandleFunc("/api/auth/signup", authHandler.Signup)
	mux.HandleFunc("/api/auth/signin", authHandler.Login)

	// Protected endpoints
	authMiddleware := middleware.JWTAuth(cfg.JWT, userRepo, appLogger)

	// Room endpoints
	mux.Handle("/api/rooms", authMiddleware(http.HandlerFunc(roomHandler.ListRooms)))
	mux.Handle("/api/rooms/", authMiddleware(http.HandlerFunc(roomHandler.HandleRoom)))

	// Booking endpoints
	mux.Handle("/api/bookings", authMiddleware(http.HandlerFunc(bookingHandler.HandleBookings)))
	mux.Handle("/api/bookings/", authMiddleware(http.HandlerFunc(bookingHandler.HandleBooking)))
	mux.Handle("/api/bookings/my-bookings", authMiddleware(http.HandlerFunc(bookingHandler.GetMyBookings)))
	mux.Handle("/api/bookings/room/", authMiddleware(http.HandlerFunc(bookingHandler.GetBookingsByRoom)))

	// Dashboard endpoint
	mux.Handle("/api/dashboard", authMiddleware(http.HandlerFunc(dashboardHandler.GetDashboard)))

	// Apply middleware chain
	handler := chain.Then(mux)

	// Create HTTP server
	server := &http.Server{
		Addr:         fmt.Sprintf(":%s", cfg.Server.Port),
		Handler:      handler,
		ReadTimeout:  15 * time.Second,
		WriteTimeout: 15 * time.Second,
		IdleTimeout:  60 * time.Second,
	}

	// Start server in goroutine
	go func() {
		appLogger.Info("HTTP server starting", map[string]interface{}{
			"address": server.Addr,
		})
		if err := server.ListenAndServe(); err != nil && err != http.ErrServerClosed {
			appLogger.Fatal("HTTP server failed", map[string]interface{}{
				"error": err.Error(),
			})
		}
	}()

	// Wait for interrupt signal
	quit := make(chan os.Signal, 1)
	signal.Notify(quit, syscall.SIGINT, syscall.SIGTERM)
	<-quit

	appLogger.Info("Shutting down server...", map[string]interface{}{
		"server": "shutdown",
	})

	// Graceful shutdown with 30 second timeout
	ctx, cancel := context.WithTimeout(context.Background(), 30*time.Second)
	defer cancel()

	if err := server.Shutdown(ctx); err != nil {
		appLogger.Error("Server forced to shutdown", map[string]interface{}{
			"error": err.Error(),
		})
	}

	appLogger.Info("Server exited", map[string]interface{}{
		"server": "gracefully",
	})
}
