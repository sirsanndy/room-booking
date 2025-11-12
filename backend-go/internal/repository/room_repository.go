package repository

import (
	"context"
	"database/sql"
	"encoding/json"
	"fmt"
	"strings"

	"meetingroom/internal/config"
	"meetingroom/internal/domain/models"
	"meetingroom/internal/logger"
)

type RoomRepository struct {
	db    *sql.DB
	cache *config.RedisCache
	log   *logger.Logger
}

func NewRoomRepository(db *sql.DB, cache *config.RedisCache, log *logger.Logger) *RoomRepository {
	return &RoomRepository{
		db:    db,
		cache: cache,
		log:   log,
	}
}

// FindAll retrieves all meeting rooms with caching
func (r *RoomRepository) FindAll(ctx context.Context) ([]*models.MeetingRoom, error) {
	// Try cache first
	cacheKey := "rooms:all"
	if r.cache != nil {
		cached, err := r.cache.Get(ctx, cacheKey)
		if err == nil {
			var rooms []*models.MeetingRoom
			if err := json.Unmarshal([]byte(cached), &rooms); err == nil {
				r.log.Debug("Rooms cache hit", nil)
				return rooms, nil
			}
		}
	}

	// Cache miss - query database
	query := `SELECT id, name, capacity, location, available, features, created_at, updated_at 
              FROM meeting_rooms ORDER BY name`

	rows, err := r.db.QueryContext(ctx, query)
	if err != nil {
		r.log.Error("Database error finding all rooms", map[string]interface{}{
			"error": err.Error(),
		})
		return nil, fmt.Errorf("database error: %w", err)
	}
	defer rows.Close()

	var rooms []*models.MeetingRoom
	for rows.Next() {
		room := &models.MeetingRoom{}
		err := rows.Scan(
			&room.ID, &room.Name, &room.Capacity, &room.Location,
			&room.Available, &room.Features, &room.CreatedAt, &room.UpdatedAt,
		)
		if err != nil {
			r.log.Error("Error scanning room row", map[string]interface{}{
				"error": err.Error(),
			})
			continue
		}
		rooms = append(rooms, room)
	}

	// Store in cache
	if r.cache != nil {
		jsonData, _ := json.Marshal(rooms)
		r.cache.Set(ctx, cacheKey, jsonData, r.cache.RoomTTL)
	}

	return rooms, nil
}

// FindByID retrieves a meeting room by ID with caching
func (r *RoomRepository) FindByID(ctx context.Context, id int64) (*models.MeetingRoom, error) {
	// Try cache first
	cacheKey := fmt.Sprintf("room:%d", id)
	if r.cache != nil {
		cached, err := r.cache.Get(ctx, cacheKey)
		if err == nil {
			var room models.MeetingRoom
			if err := json.Unmarshal([]byte(cached), &room); err == nil {
				r.log.Debug("Room cache hit", map[string]interface{}{"roomId": id})
				return &room, nil
			}
		}
	}

	// Cache miss - query database
	query := `SELECT id, name, capacity, location, available, features, created_at, updated_at 
              FROM meeting_rooms WHERE id = $1`

	room := &models.MeetingRoom{}
	err := r.db.QueryRowContext(ctx, query, id).Scan(
		&room.ID, &room.Name, &room.Capacity, &room.Location,
		&room.Available, &room.Features, &room.CreatedAt, &room.UpdatedAt,
	)

	if err == sql.ErrNoRows {
		return nil, fmt.Errorf("room not found")
	}
	if err != nil {
		r.log.Error("Database error finding room by ID", map[string]interface{}{
			"roomId": id,
			"error":  err.Error(),
		})
		return nil, fmt.Errorf("database error: %w", err)
	}

	// Store in cache
	if r.cache != nil {
		jsonData, _ := json.Marshal(room)
		r.cache.Set(ctx, cacheKey, jsonData, r.cache.RoomTTL)
	}

	return room, nil
}

// FindAvailable retrieves only available meeting rooms
func (r *RoomRepository) FindAvailable(ctx context.Context) ([]*models.MeetingRoom, error) {
	query := `SELECT id, name, capacity, location, available, features, created_at, updated_at 
              FROM meeting_rooms WHERE available = true ORDER BY name`

	rows, err := r.db.QueryContext(ctx, query)
	if err != nil {
		r.log.Error("Database error finding available rooms", map[string]interface{}{
			"error": err.Error(),
		})
		return nil, fmt.Errorf("database error: %w", err)
	}
	defer rows.Close()

	var rooms []*models.MeetingRoom
	for rows.Next() {
		room := &models.MeetingRoom{}
		err := rows.Scan(
			&room.ID, &room.Name, &room.Capacity, &room.Location,
			&room.Available, &room.Features, &room.CreatedAt, &room.UpdatedAt,
		)
		if err != nil {
			r.log.Error("Error scanning room row", map[string]interface{}{
				"error": err.Error(),
			})
			continue
		}
		rooms = append(rooms, room)
	}

	return rooms, nil
}

// Create inserts a new meeting room
func (r *RoomRepository) Create(ctx context.Context, room *models.MeetingRoom) error {
	query := `INSERT INTO meeting_rooms (name, capacity, location, available, features) 
              VALUES ($1, $2, $3, $4, $5) 
              RETURNING id, created_at, updated_at`

	err := r.db.QueryRowContext(ctx, query,
		room.Name, room.Capacity, room.Location, room.Available, room.Features,
	).Scan(&room.ID, &room.CreatedAt, &room.UpdatedAt)

	if err != nil {
		r.log.Error("Database error creating room", map[string]interface{}{
			"roomName": room.Name,
			"error":    err.Error(),
		})
		return fmt.Errorf("failed to create room: %w", err)
	}

	// Invalidate cache
	r.invalidateCache(ctx)

	r.log.Info("Room created", map[string]interface{}{
		"roomId":   room.ID,
		"roomName": room.Name,
	})

	return nil
}

// Update updates an existing meeting room
func (r *RoomRepository) Update(ctx context.Context, room *models.MeetingRoom) error {
	query := `UPDATE meeting_rooms 
              SET name = $1, capacity = $2, location = $3, available = $4, features = $5
              WHERE id = $6`

	result, err := r.db.ExecContext(ctx, query,
		room.Name, room.Capacity, room.Location, room.Available, room.Features, room.ID,
	)

	if err != nil {
		r.log.Error("Database error updating room", map[string]interface{}{
			"roomId": room.ID,
			"error":  err.Error(),
		})
		return fmt.Errorf("failed to update room: %w", err)
	}

	rowsAffected, _ := result.RowsAffected()
	if rowsAffected == 0 {
		return fmt.Errorf("room not found")
	}

	// Invalidate cache
	r.invalidateCache(ctx)

	r.log.Info("Room updated", map[string]interface{}{
		"roomId": room.ID,
	})

	return nil
}

// Delete removes a meeting room
func (r *RoomRepository) Delete(ctx context.Context, id int64) error {
	query := `DELETE FROM meeting_rooms WHERE id = $1`

	result, err := r.db.ExecContext(ctx, query, id)
	if err != nil {
		// Check if it's a foreign key constraint error
		if strings.Contains(err.Error(), "foreign key") {
			return fmt.Errorf("cannot delete room with existing bookings")
		}
		r.log.Error("Database error deleting room", map[string]interface{}{
			"roomId": id,
			"error":  err.Error(),
		})
		return fmt.Errorf("failed to delete room: %w", err)
	}

	rowsAffected, _ := result.RowsAffected()
	if rowsAffected == 0 {
		return fmt.Errorf("room not found")
	}

	// Invalidate cache
	r.invalidateCache(ctx)

	r.log.Info("Room deleted", map[string]interface{}{
		"roomId": id,
	})

	return nil
}

// FindByIDs retrieves multiple rooms by IDs (for N+1 prevention)
func (r *RoomRepository) FindByIDs(ctx context.Context, ids []int64) (map[int64]*models.MeetingRoom, error) {
	if len(ids) == 0 {
		return make(map[int64]*models.MeetingRoom), nil
	}

	query := `SELECT id, name, capacity, location, available, features, created_at, updated_at 
              FROM meeting_rooms WHERE id = ANY($1)`

	rows, err := r.db.QueryContext(ctx, query, ids)
	if err != nil {
		r.log.Error("Database error finding rooms by IDs", map[string]interface{}{
			"error": err.Error(),
		})
		return nil, fmt.Errorf("database error: %w", err)
	}
	defer rows.Close()

	rooms := make(map[int64]*models.MeetingRoom)
	for rows.Next() {
		room := &models.MeetingRoom{}
		err := rows.Scan(
			&room.ID, &room.Name, &room.Capacity, &room.Location,
			&room.Available, &room.Features, &room.CreatedAt, &room.UpdatedAt,
		)
		if err != nil {
			r.log.Error("Error scanning room row", map[string]interface{}{
				"error": err.Error(),
			})
			continue
		}
		rooms[room.ID] = room
	}

	return rooms, nil
}

// invalidateCache clears all room-related cache entries
func (r *RoomRepository) invalidateCache(ctx context.Context) {
	if r.cache != nil {
		r.cache.DeletePattern(ctx, "room:*")
		r.cache.Delete(ctx, "rooms:all")
	}
}
