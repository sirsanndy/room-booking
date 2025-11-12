package repository

import (
	"context"
	"database/sql"
	"fmt"
	"time"

	"meetingroom/internal/domain/models"
	"meetingroom/internal/logger"
)

type BookingRepository struct {
	db  *sql.DB
	log *logger.Logger
}

func NewBookingRepository(db *sql.DB, log *logger.Logger) *BookingRepository {
	return &BookingRepository{
		db:  db,
		log: log,
	}
}

// FindByID retrieves a booking by ID
func (r *BookingRepository) FindByID(ctx context.Context, id int64) (*models.Booking, error) {
	query := `SELECT id, room_id, user_id, start_time, end_time, title, description, status, version, created_at, updated_at 
              FROM bookings WHERE id = $1`

	booking := &models.Booking{}
	err := r.db.QueryRowContext(ctx, query, id).Scan(
		&booking.ID, &booking.RoomID, &booking.UserID, &booking.StartTime, &booking.EndTime,
		&booking.Title, &booking.Description, &booking.Status, &booking.Version,
		&booking.CreatedAt, &booking.UpdatedAt,
	)

	if err == sql.ErrNoRows {
		return nil, fmt.Errorf("booking not found")
	}
	if err != nil {
		r.log.Error("Database error finding booking by ID", map[string]interface{}{
			"bookingId": id,
			"error":     err.Error(),
		})
		return nil, fmt.Errorf("database error: %w", err)
	}

	return booking, nil
}

// FindByIDWithDetails retrieves a booking with room and user details
func (r *BookingRepository) FindByIDWithDetails(ctx context.Context, id int64) (*models.BookingWithDetails, error) {
	query := `SELECT b.id, b.room_id, b.user_id, b.start_time, b.end_time, b.title, b.description, 
                     b.status, b.version, b.created_at, b.updated_at,
                     r.id, r.name, r.capacity, r.location, r.available, r.features, r.created_at, r.updated_at,
                     u.id, u.username, u.email, u.roles, u.enabled, u.created_at, u.updated_at
              FROM bookings b
              JOIN meeting_rooms r ON b.room_id = r.id
              JOIN users u ON b.user_id = u.id
              WHERE b.id = $1`

	booking := &models.BookingWithDetails{
		Room: &models.MeetingRoom{},
		User: &models.User{},
	}

	var userPassword string
	err := r.db.QueryRowContext(ctx, query, id).Scan(
		&booking.ID, &booking.RoomID, &booking.UserID, &booking.StartTime, &booking.EndTime,
		&booking.Title, &booking.Description, &booking.Status, &booking.Version,
		&booking.CreatedAt, &booking.UpdatedAt,
		&booking.Room.ID, &booking.Room.Name, &booking.Room.Capacity, &booking.Room.Location,
		&booking.Room.Available, &booking.Room.Features, &booking.Room.CreatedAt, &booking.Room.UpdatedAt,
		&booking.User.ID, &booking.User.Username, &booking.User.Email,
		&booking.User.Roles, &booking.User.Enabled, &booking.User.CreatedAt, &booking.User.UpdatedAt,
	)

	_ = userPassword // Ignore password

	if err == sql.ErrNoRows {
		return nil, fmt.Errorf("booking not found")
	}
	if err != nil {
		r.log.Error("Database error finding booking with details", map[string]interface{}{
			"bookingId": id,
			"error":     err.Error(),
		})
		return nil, fmt.Errorf("database error: %w", err)
	}

	return booking, nil
}

// FindByUserID retrieves all bookings for a user
func (r *BookingRepository) FindByUserID(ctx context.Context, userID int64) ([]*models.BookingWithDetails, error) {
	query := `SELECT b.id, b.room_id, b.user_id, b.start_time, b.end_time, b.title, b.description, 
                     b.status, b.version, b.created_at, b.updated_at,
                     r.id, r.name, r.capacity, r.location, r.available, r.features, r.created_at, r.updated_at,
                     u.id, u.username, u.email, u.roles, u.enabled, u.created_at, u.updated_at
              FROM bookings b
              JOIN meeting_rooms r ON b.room_id = r.id
              JOIN users u ON b.user_id = u.id
              WHERE b.user_id = $1
              ORDER BY b.start_time DESC`

	rows, err := r.db.QueryContext(ctx, query, userID)
	if err != nil {
		r.log.Error("Database error finding bookings by user", map[string]interface{}{
			"userId": userID,
			"error":  err.Error(),
		})
		return nil, fmt.Errorf("database error: %w", err)
	}
	defer rows.Close()

	return r.scanBookingsWithDetails(rows)
}

// FindByRoomAndTimeRange retrieves bookings for a room within a time range
func (r *BookingRepository) FindByRoomAndTimeRange(ctx context.Context, roomID int64, start, end time.Time) ([]*models.Booking, error) {
	query := `SELECT id, room_id, user_id, start_time, end_time, title, description, status, version, created_at, updated_at 
              FROM bookings 
              WHERE room_id = $1 AND start_time < $3 AND end_time > $2 AND status = 'CONFIRMED'
              ORDER BY start_time`

	rows, err := r.db.QueryContext(ctx, query, roomID, start, end)
	if err != nil {
		r.log.Error("Database error finding bookings by room and time", map[string]interface{}{
			"roomId": roomID,
			"error":  err.Error(),
		})
		return nil, fmt.Errorf("database error: %w", err)
	}
	defer rows.Close()

	var bookings []*models.Booking
	for rows.Next() {
		booking := &models.Booking{}
		err := rows.Scan(
			&booking.ID, &booking.RoomID, &booking.UserID, &booking.StartTime, &booking.EndTime,
			&booking.Title, &booking.Description, &booking.Status, &booking.Version,
			&booking.CreatedAt, &booking.UpdatedAt,
		)
		if err != nil {
			r.log.Error("Error scanning booking row", map[string]interface{}{
				"error": err.Error(),
			})
			continue
		}
		bookings = append(bookings, booking)
	}

	return bookings, nil
}

// FindOverlapping checks for overlapping bookings (for conflict detection)
func (r *BookingRepository) FindOverlapping(ctx context.Context, roomID int64, start, end time.Time, excludeID int64) ([]*models.Booking, error) {
	query := `SELECT id, room_id, user_id, start_time, end_time, title, description, status, version, created_at, updated_at 
              FROM bookings 
              WHERE room_id = $1 
              AND start_time < $3 
              AND end_time > $2 
              AND status = 'CONFIRMED'
              AND id != $4
              FOR UPDATE`

	rows, err := r.db.QueryContext(ctx, query, roomID, start, end, excludeID)
	if err != nil {
		r.log.Error("Database error finding overlapping bookings", map[string]interface{}{
			"roomId": roomID,
			"error":  err.Error(),
		})
		return nil, fmt.Errorf("database error: %w", err)
	}
	defer rows.Close()

	var bookings []*models.Booking
	for rows.Next() {
		booking := &models.Booking{}
		err := rows.Scan(
			&booking.ID, &booking.RoomID, &booking.UserID, &booking.StartTime, &booking.EndTime,
			&booking.Title, &booking.Description, &booking.Status, &booking.Version,
			&booking.CreatedAt, &booking.UpdatedAt,
		)
		if err != nil {
			r.log.Error("Error scanning booking row", map[string]interface{}{
				"error": err.Error(),
			})
			continue
		}
		bookings = append(bookings, booking)
	}

	return bookings, nil
}

// FindUpcoming retrieves upcoming bookings
func (r *BookingRepository) FindUpcoming(ctx context.Context, limit int) ([]*models.BookingWithDetails, error) {
	query := `SELECT b.id, b.room_id, b.user_id, b.start_time, b.end_time, b.title, b.description, 
                     b.status, b.version, b.created_at, b.updated_at,
                     r.id, r.name, r.capacity, r.location, r.available, r.features, r.created_at, r.updated_at,
                     u.id, u.username, u.email, u.roles, u.enabled, u.created_at, u.updated_at
              FROM bookings b
              JOIN meeting_rooms r ON b.room_id = r.id
              JOIN users u ON b.user_id = u.id
              WHERE b.start_time > NOW() AND b.status = 'CONFIRMED'
              ORDER BY b.start_time ASC
              LIMIT $1`

	rows, err := r.db.QueryContext(ctx, query, limit)
	if err != nil {
		r.log.Error("Database error finding upcoming bookings", map[string]interface{}{
			"error": err.Error(),
		})
		return nil, fmt.Errorf("database error: %w", err)
	}
	defer rows.Close()

	return r.scanBookingsWithDetails(rows)
}

// FindRecent retrieves recent bookings
func (r *BookingRepository) FindRecent(ctx context.Context, limit int) ([]*models.BookingWithDetails, error) {
	query := `SELECT b.id, b.room_id, b.user_id, b.start_time, b.end_time, b.title, b.description, 
                     b.status, b.version, b.created_at, b.updated_at,
                     r.id, r.name, r.capacity, r.location, r.available, r.features, r.created_at, r.updated_at,
                     u.id, u.username, u.email, u.roles, u.enabled, u.created_at, u.updated_at
              FROM bookings b
              JOIN meeting_rooms r ON b.room_id = r.id
              JOIN users u ON b.user_id = u.id
              WHERE b.status = 'CONFIRMED'
              ORDER BY b.created_at DESC
              LIMIT $1`

	rows, err := r.db.QueryContext(ctx, query, limit)
	if err != nil {
		r.log.Error("Database error finding recent bookings", map[string]interface{}{
			"error": err.Error(),
		})
		return nil, fmt.Errorf("database error: %w", err)
	}
	defer rows.Close()

	return r.scanBookingsWithDetails(rows)
}

// Create inserts a new booking
func (r *BookingRepository) Create(ctx context.Context, booking *models.Booking) error {
	query := `INSERT INTO bookings (room_id, user_id, start_time, end_time, title, description, status, version) 
              VALUES ($1, $2, $3, $4, $5, $6, $7, 0) 
              RETURNING id, created_at, updated_at`

	err := r.db.QueryRowContext(ctx, query,
		booking.RoomID, booking.UserID, booking.StartTime, booking.EndTime,
		booking.Title, booking.Description, booking.Status,
	).Scan(&booking.ID, &booking.CreatedAt, &booking.UpdatedAt)

	if err != nil {
		r.log.Error("Database error creating booking", map[string]interface{}{
			"roomId": booking.RoomID,
			"userId": booking.UserID,
			"error":  err.Error(),
		})
		return fmt.Errorf("failed to create booking: %w", err)
	}

	r.log.Info("Booking created", map[string]interface{}{
		"bookingId": booking.ID,
		"roomId":    booking.RoomID,
		"userId":    booking.UserID,
	})

	return nil
}

// Update updates a booking with optimistic locking
func (r *BookingRepository) Update(ctx context.Context, booking *models.Booking) error {
	query := `UPDATE bookings 
              SET room_id = $1, start_time = $2, end_time = $3, title = $4, description = $5, 
                  status = $6, version = version + 1
              WHERE id = $7 AND version = $8`

	result, err := r.db.ExecContext(ctx, query,
		booking.RoomID, booking.StartTime, booking.EndTime, booking.Title,
		booking.Description, booking.Status, booking.ID, booking.Version,
	)

	if err != nil {
		r.log.Error("Database error updating booking", map[string]interface{}{
			"bookingId": booking.ID,
			"error":     err.Error(),
		})
		return fmt.Errorf("failed to update booking: %w", err)
	}

	rowsAffected, _ := result.RowsAffected()
	if rowsAffected == 0 {
		return fmt.Errorf("booking was modified by another transaction (optimistic lock failed)")
	}

	booking.Version++

	r.log.Info("Booking updated", map[string]interface{}{
		"bookingId": booking.ID,
	})

	return nil
}

// Cancel cancels a booking with optimistic locking
func (r *BookingRepository) Cancel(ctx context.Context, id int64, version int) error {
	query := `UPDATE bookings 
              SET status = 'CANCELLED', version = version + 1
              WHERE id = $1 AND version = $2`

	result, err := r.db.ExecContext(ctx, query, id, version)
	if err != nil {
		r.log.Error("Database error cancelling booking", map[string]interface{}{
			"bookingId": id,
			"error":     err.Error(),
		})
		return fmt.Errorf("failed to cancel booking: %w", err)
	}

	rowsAffected, _ := result.RowsAffected()
	if rowsAffected == 0 {
		return fmt.Errorf("booking was modified by another transaction (optimistic lock failed)")
	}

	r.log.Info("Booking cancelled", map[string]interface{}{
		"bookingId": id,
	})

	return nil
}

// CountByStatus counts bookings by status
func (r *BookingRepository) CountByStatus(ctx context.Context) (map[string]int, error) {
	query := `SELECT status, COUNT(*) FROM bookings GROUP BY status`

	rows, err := r.db.QueryContext(ctx, query)
	if err != nil {
		r.log.Error("Database error counting bookings by status", map[string]interface{}{
			"error": err.Error(),
		})
		return nil, fmt.Errorf("database error: %w", err)
	}
	defer rows.Close()

	counts := make(map[string]int)
	for rows.Next() {
		var status string
		var count int
		if err := rows.Scan(&status, &count); err != nil {
			continue
		}
		counts[status] = count
	}

	return counts, nil
}

// CountByRoom counts bookings by room
func (r *BookingRepository) CountByRoom(ctx context.Context) (map[string]int, error) {
	query := `SELECT r.name, COUNT(b.id) 
              FROM meeting_rooms r
              LEFT JOIN bookings b ON r.id = b.room_id AND b.status = 'CONFIRMED'
              GROUP BY r.name`

	rows, err := r.db.QueryContext(ctx, query)
	if err != nil {
		r.log.Error("Database error counting bookings by room", map[string]interface{}{
			"error": err.Error(),
		})
		return nil, fmt.Errorf("database error: %w", err)
	}
	defer rows.Close()

	counts := make(map[string]int)
	for rows.Next() {
		var roomName string
		var count int
		if err := rows.Scan(&roomName, &count); err != nil {
			continue
		}
		counts[roomName] = count
	}

	return counts, nil
}

// CountTotal counts total bookings
func (r *BookingRepository) CountTotal(ctx context.Context) (int, error) {
	query := `SELECT COUNT(*) FROM bookings`

	var count int
	err := r.db.QueryRowContext(ctx, query).Scan(&count)
	if err != nil {
		r.log.Error("Database error counting total bookings", map[string]interface{}{
			"error": err.Error(),
		})
		return 0, fmt.Errorf("database error: %w", err)
	}

	return count, nil
}

// CountActive counts active bookings
func (r *BookingRepository) CountActive(ctx context.Context) (int, error) {
	query := `SELECT COUNT(*) FROM bookings WHERE status = 'CONFIRMED' AND end_time > NOW()`

	var count int
	err := r.db.QueryRowContext(ctx, query).Scan(&count)
	if err != nil {
		r.log.Error("Database error counting active bookings", map[string]interface{}{
			"error": err.Error(),
		})
		return 0, fmt.Errorf("database error: %w", err)
	}

	return count, nil
}

// Helper function to scan bookings with details
func (r *BookingRepository) scanBookingsWithDetails(rows *sql.Rows) ([]*models.BookingWithDetails, error) {
	var bookings []*models.BookingWithDetails
	for rows.Next() {
		booking := &models.BookingWithDetails{
			Room: &models.MeetingRoom{},
			User: &models.User{},
		}

		var userPassword string
		err := rows.Scan(
			&booking.ID, &booking.RoomID, &booking.UserID, &booking.StartTime, &booking.EndTime,
			&booking.Title, &booking.Description, &booking.Status, &booking.Version,
			&booking.CreatedAt, &booking.UpdatedAt,
			&booking.Room.ID, &booking.Room.Name, &booking.Room.Capacity, &booking.Room.Location,
			&booking.Room.Available, &booking.Room.Features, &booking.Room.CreatedAt, &booking.Room.UpdatedAt,
			&booking.User.ID, &booking.User.Username, &booking.User.Email,
			&booking.User.Roles, &booking.User.Enabled, &booking.User.CreatedAt, &booking.User.UpdatedAt,
		)

		_ = userPassword // Ignore password

		if err != nil {
			r.log.Error("Error scanning booking with details", map[string]interface{}{
				"error": err.Error(),
			})
			continue
		}
		bookings = append(bookings, booking)
	}

	return bookings, nil
}
