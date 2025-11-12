package service

import (
	"context"
	"database/sql"
	"fmt"
	"time"

	"meetingroom/internal/domain/models"
	"meetingroom/internal/logger"
	"meetingroom/internal/repository"
)

type BookingService struct {
	bookingRepo *repository.BookingRepository
	roomRepo    *repository.RoomRepository
	userRepo    *repository.UserRepository
	log         *logger.Logger
	db          *sql.DB
}

func NewBookingService(
	bookingRepo *repository.BookingRepository,
	roomRepo *repository.RoomRepository,
	userRepo *repository.UserRepository,
	db *sql.DB,
	log *logger.Logger,
) *BookingService {
	return &BookingService{
		bookingRepo: bookingRepo,
		roomRepo:    roomRepo,
		userRepo:    userRepo,
		db:          db,
		log:         log,
	}
}

// Create creates a new booking with conflict checking
func (s *BookingService) Create(ctx context.Context, userID int64, booking *models.Booking) (*models.BookingWithDetails, error) {
	// Validate input
	if err := s.validateBooking(booking); err != nil {
		return nil, err
	}

	// Set user ID
	booking.UserID = userID
	booking.Status = models.BookingStatusConfirmed

	// Begin transaction
	tx, err := s.db.BeginTx(ctx, nil)
	if err != nil {
		return nil, fmt.Errorf("failed to begin transaction: %w", err)
	}
	defer tx.Rollback()

	// Check if room exists and is available
	room, err := s.roomRepo.FindByID(ctx, booking.RoomID)
	if err != nil {
		return nil, fmt.Errorf("room not found")
	}
	if !room.Available {
		return nil, fmt.Errorf("room is not available")
	}

	// Check for overlapping bookings with pessimistic lock
	overlapping, err := s.bookingRepo.FindOverlapping(ctx, booking.RoomID, booking.StartTime, booking.EndTime, 0)
	if err != nil {
		s.log.Error("Failed to check overlapping bookings", map[string]interface{}{
			"error": err.Error(),
		})
		return nil, fmt.Errorf("failed to check availability")
	}

	if len(overlapping) > 0 {
		return nil, fmt.Errorf("room is already booked for the selected time")
	}

	// Create booking
	if err := s.bookingRepo.Create(ctx, booking); err != nil {
		s.log.Error("Failed to create booking", map[string]interface{}{
			"userId": userID,
			"roomId": booking.RoomID,
			"error":  err.Error(),
		})
		return nil, fmt.Errorf("failed to create booking: %w", err)
	}

	// Commit transaction
	if err := tx.Commit(); err != nil {
		return nil, fmt.Errorf("failed to commit transaction: %w", err)
	}

	// Get booking with details
	bookingDetails, err := s.bookingRepo.FindByIDWithDetails(ctx, booking.ID)
	if err != nil {
		s.log.Error("Failed to get booking details", map[string]interface{}{
			"bookingId": booking.ID,
			"error":     err.Error(),
		})
		return nil, fmt.Errorf("booking created but failed to retrieve details")
	}

	s.log.Info("Booking created successfully", map[string]interface{}{
		"bookingId": booking.ID,
		"userId":    userID,
		"roomId":    booking.RoomID,
	})

	return bookingDetails, nil
}

// GetByID retrieves a booking by ID
func (s *BookingService) GetByID(ctx context.Context, id int64) (*models.BookingWithDetails, error) {
	booking, err := s.bookingRepo.FindByIDWithDetails(ctx, id)
	if err != nil {
		s.log.Error("Failed to get booking by ID", map[string]interface{}{
			"bookingId": id,
			"error":     err.Error(),
		})
		return nil, fmt.Errorf("failed to get booking: %w", err)
	}

	return booking, nil
}

// GetMyBookings retrieves all bookings for a user
func (s *BookingService) GetMyBookings(ctx context.Context, userID int64) ([]*models.BookingWithDetails, error) {
	bookings, err := s.bookingRepo.FindByUserID(ctx, userID)
	if err != nil {
		s.log.Error("Failed to get user bookings", map[string]interface{}{
			"userId": userID,
			"error":  err.Error(),
		})
		return nil, fmt.Errorf("failed to get bookings: %w", err)
	}

	return bookings, nil
}

// GetByRoomID retrieves bookings for a room within a time range
func (s *BookingService) GetByRoomID(ctx context.Context, roomID int64, start, end time.Time) ([]*models.Booking, error) {
	bookings, err := s.bookingRepo.FindByRoomAndTimeRange(ctx, roomID, start, end)
	if err != nil {
		s.log.Error("Failed to get room bookings", map[string]interface{}{
			"roomId": roomID,
			"error":  err.Error(),
		})
		return nil, fmt.Errorf("failed to get bookings: %w", err)
	}

	return bookings, nil
}

// Update updates an existing booking
func (s *BookingService) Update(ctx context.Context, id int64, userID int64, booking *models.Booking) (*models.BookingWithDetails, error) {
	// Validate input
	if err := s.validateBooking(booking); err != nil {
		return nil, err
	}

	// Get existing booking
	existing, err := s.bookingRepo.FindByID(ctx, id)
	if err != nil {
		return nil, fmt.Errorf("booking not found")
	}

	// Check ownership
	if existing.UserID != userID {
		return nil, fmt.Errorf("unauthorized: you can only update your own bookings")
	}

	// Check for overlapping bookings (excluding current booking)
	overlapping, err := s.bookingRepo.FindOverlapping(ctx, booking.RoomID, booking.StartTime, booking.EndTime, id)
	if err != nil {
		s.log.Error("Failed to check overlapping bookings", map[string]interface{}{
			"error": err.Error(),
		})
		return nil, fmt.Errorf("failed to check availability")
	}

	if len(overlapping) > 0 {
		return nil, fmt.Errorf("room is already booked for the selected time")
	}

	// Update fields
	existing.RoomID = booking.RoomID
	existing.StartTime = booking.StartTime
	existing.EndTime = booking.EndTime
	existing.Title = booking.Title
	existing.Description = booking.Description

	// Update with optimistic locking
	if err := s.bookingRepo.Update(ctx, existing); err != nil {
		s.log.Error("Failed to update booking", map[string]interface{}{
			"bookingId": id,
			"error":     err.Error(),
		})
		return nil, fmt.Errorf("failed to update booking: %w", err)
	}

	// Get updated booking with details
	bookingDetails, err := s.bookingRepo.FindByIDWithDetails(ctx, id)
	if err != nil {
		return nil, fmt.Errorf("booking updated but failed to retrieve details")
	}

	s.log.Info("Booking updated successfully", map[string]interface{}{
		"bookingId": id,
	})

	return bookingDetails, nil
}

// Cancel cancels a booking
func (s *BookingService) Cancel(ctx context.Context, id int64, userID int64, version int) error {
	// Get existing booking
	existing, err := s.bookingRepo.FindByID(ctx, id)
	if err != nil {
		return fmt.Errorf("booking not found")
	}

	// Check ownership
	if existing.UserID != userID {
		return fmt.Errorf("unauthorized: you can only cancel your own bookings")
	}

	// Check if already cancelled
	if existing.Status == models.BookingStatusCancelled {
		return fmt.Errorf("booking is already cancelled")
	}

	// Cancel with optimistic locking
	if err := s.bookingRepo.Cancel(ctx, id, version); err != nil {
		s.log.Error("Failed to cancel booking", map[string]interface{}{
			"bookingId": id,
			"error":     err.Error(),
		})
		return fmt.Errorf("failed to cancel booking: %w", err)
	}

	s.log.Info("Booking cancelled successfully", map[string]interface{}{
		"bookingId": id,
	})

	return nil
}

// CheckAvailability checks if a room is available for a given time range
func (s *BookingService) CheckAvailability(ctx context.Context, roomID int64, start, end time.Time) (bool, error) {
	overlapping, err := s.bookingRepo.FindOverlapping(ctx, roomID, start, end, 0)
	if err != nil {
		return false, fmt.Errorf("failed to check availability: %w", err)
	}

	return len(overlapping) == 0, nil
}

// validateBooking validates booking input
func (s *BookingService) validateBooking(booking *models.Booking) error {
	if booking.RoomID == 0 {
		return fmt.Errorf("room ID is required")
	}
	if booking.Title == "" {
		return fmt.Errorf("title is required")
	}
	if booking.StartTime.IsZero() {
		return fmt.Errorf("start time is required")
	}
	if booking.EndTime.IsZero() {
		return fmt.Errorf("end time is required")
	}

	// Validate time range
	if booking.EndTime.Before(booking.StartTime) || booking.EndTime.Equal(booking.StartTime) {
		return fmt.Errorf("end time must be after start time")
	}

	// Validate booking duration (30 min to 8 hours)
	duration := booking.EndTime.Sub(booking.StartTime)
	if duration < 30*time.Minute {
		return fmt.Errorf("booking duration must be at least 30 minutes")
	}
	if duration > 8*time.Hour {
		return fmt.Errorf("booking duration cannot exceed 8 hours")
	}

	// Cannot book in the past
	if booking.StartTime.Before(time.Now()) {
		return fmt.Errorf("cannot book in the past")
	}

	return nil
}
