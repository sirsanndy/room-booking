package service

import (
	"context"
	"fmt"

	"meetingroom/internal/config"
	"meetingroom/internal/domain/models"
	"meetingroom/internal/logger"
	"meetingroom/internal/repository"
)

type RoomService struct {
	roomRepo *repository.RoomRepository
	cache    *config.RedisCache
	log      *logger.Logger
}

func NewRoomService(roomRepo *repository.RoomRepository, cache *config.RedisCache, log *logger.Logger) *RoomService {
	return &RoomService{
		roomRepo: roomRepo,
		cache:    cache,
		log:      log,
	}
}

// GetAll retrieves all meeting rooms
func (s *RoomService) GetAll(ctx context.Context) ([]*models.MeetingRoom, error) {
	rooms, err := s.roomRepo.FindAll(ctx)
	if err != nil {
		s.log.Error("Failed to get all rooms", map[string]interface{}{
			"error": err.Error(),
		})
		return nil, fmt.Errorf("failed to get rooms: %w", err)
	}

	return rooms, nil
}

// GetByID retrieves a room by ID
func (s *RoomService) GetByID(ctx context.Context, id int64) (*models.MeetingRoom, error) {
	room, err := s.roomRepo.FindByID(ctx, id)
	if err != nil {
		s.log.Error("Failed to get room by ID", map[string]interface{}{
			"roomId": id,
			"error":  err.Error(),
		})
		return nil, fmt.Errorf("failed to get room: %w", err)
	}

	return room, nil
}

// GetAvailable retrieves only available rooms
func (s *RoomService) GetAvailable(ctx context.Context) ([]*models.MeetingRoom, error) {
	rooms, err := s.roomRepo.FindAvailable(ctx)
	if err != nil {
		s.log.Error("Failed to get available rooms", map[string]interface{}{
			"error": err.Error(),
		})
		return nil, fmt.Errorf("failed to get available rooms: %w", err)
	}

	return rooms, nil
}

// Create creates a new meeting room
func (s *RoomService) Create(ctx context.Context, room *models.MeetingRoom) (*models.MeetingRoom, error) {
	// Validate input
	if err := s.validateRoom(room); err != nil {
		return nil, err
	}

	if err := s.roomRepo.Create(ctx, room); err != nil {
		s.log.Error("Failed to create room", map[string]interface{}{
			"roomName": room.Name,
			"error":    err.Error(),
		})
		return nil, fmt.Errorf("failed to create room: %w", err)
	}

	s.log.Info("Room created successfully", map[string]interface{}{
		"roomId":   room.ID,
		"roomName": room.Name,
	})

	return room, nil
}

// Update updates an existing meeting room
func (s *RoomService) Update(ctx context.Context, id int64, room *models.MeetingRoom) (*models.MeetingRoom, error) {
	// Validate input
	if err := s.validateRoom(room); err != nil {
		return nil, err
	}

	// Check if room exists
	existing, err := s.roomRepo.FindByID(ctx, id)
	if err != nil {
		return nil, fmt.Errorf("room not found")
	}

	// Update fields
	existing.Name = room.Name
	existing.Capacity = room.Capacity
	existing.Location = room.Location
	existing.Available = room.Available
	existing.Features = room.Features

	if err := s.roomRepo.Update(ctx, existing); err != nil {
		s.log.Error("Failed to update room", map[string]interface{}{
			"roomId": id,
			"error":  err.Error(),
		})
		return nil, fmt.Errorf("failed to update room: %w", err)
	}

	s.log.Info("Room updated successfully", map[string]interface{}{
		"roomId": id,
	})

	return existing, nil
}

// Delete deletes a meeting room
func (s *RoomService) Delete(ctx context.Context, id int64) error {
	// Check if room exists
	_, err := s.roomRepo.FindByID(ctx, id)
	if err != nil {
		return fmt.Errorf("room not found")
	}

	if err := s.roomRepo.Delete(ctx, id); err != nil {
		s.log.Error("Failed to delete room", map[string]interface{}{
			"roomId": id,
			"error":  err.Error(),
		})
		return fmt.Errorf("failed to delete room: %w", err)
	}

	s.log.Info("Room deleted successfully", map[string]interface{}{
		"roomId": id,
	})

	return nil
}

// validateRoom validates room input
func (s *RoomService) validateRoom(room *models.MeetingRoom) error {
	if room.Name == "" {
		return fmt.Errorf("room name is required")
	}
	if room.Capacity <= 0 {
		return fmt.Errorf("room capacity must be greater than 0")
	}
	if room.Location == "" {
		return fmt.Errorf("room location is required")
	}

	return nil
}
