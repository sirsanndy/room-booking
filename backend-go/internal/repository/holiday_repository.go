package repository

import (
	"context"
	"database/sql"
	"encoding/json"
	"fmt"
	"time"

	"meetingroom/internal/config"
	"meetingroom/internal/domain/models"
	"meetingroom/internal/logger"
)

type HolidayRepository struct {
	db    *sql.DB
	cache *config.RedisCache
	log   *logger.Logger
}

func NewHolidayRepository(db *sql.DB, cache *config.RedisCache, log *logger.Logger) *HolidayRepository {
	return &HolidayRepository{
		db:    db,
		cache: cache,
		log:   log,
	}
}

// FindAll retrieves all holidays with caching
func (r *HolidayRepository) FindAll(ctx context.Context) ([]*models.Holiday, error) {
	// Try cache first
	cacheKey := "holidays:all"
	if r.cache != nil {
		cached, err := r.cache.Get(ctx, cacheKey)
		if err == nil {
			var holidays []*models.Holiday
			if err := json.Unmarshal([]byte(cached), &holidays); err == nil {
				r.log.Debug("Holidays cache hit", nil)
				return holidays, nil
			}
		}
	}

	// Cache miss - query database
	query := `SELECT id, name, date, type, created_at FROM holidays ORDER BY date`

	rows, err := r.db.QueryContext(ctx, query)
	if err != nil {
		r.log.Error("Database error finding all holidays", map[string]interface{}{
			"error": err.Error(),
		})
		return nil, fmt.Errorf("database error: %w", err)
	}
	defer rows.Close()

	var holidays []*models.Holiday
	for rows.Next() {
		holiday := &models.Holiday{}
		var createdAt time.Time
		err := rows.Scan(&holiday.ID, &holiday.Name, &holiday.Date, &holiday.Type, &createdAt)
		if err != nil {
			r.log.Error("Error scanning holiday row", map[string]interface{}{
				"error": err.Error(),
			})
			continue
		}
		holidays = append(holidays, holiday)
	}

	// Store in cache
	if r.cache != nil {
		jsonData, _ := json.Marshal(holidays)
		r.cache.Set(ctx, cacheKey, jsonData, r.cache.HolidayTTL)
	}

	return holidays, nil
}

// FindUpcoming retrieves upcoming holidays
func (r *HolidayRepository) FindUpcoming(ctx context.Context, limit int) ([]*models.Holiday, error) {
	query := `SELECT id, name, date, type, created_at 
              FROM holidays 
              WHERE date >= CURRENT_DATE 
              ORDER BY date 
              LIMIT $1`

	rows, err := r.db.QueryContext(ctx, query, limit)
	if err != nil {
		r.log.Error("Database error finding upcoming holidays", map[string]interface{}{
			"error": err.Error(),
		})
		return nil, fmt.Errorf("database error: %w", err)
	}
	defer rows.Close()

	var holidays []*models.Holiday
	for rows.Next() {
		holiday := &models.Holiday{}
		var createdAt time.Time
		err := rows.Scan(&holiday.ID, &holiday.Name, &holiday.Date, &holiday.Type, &createdAt)
		if err != nil {
			r.log.Error("Error scanning holiday row", map[string]interface{}{
				"error": err.Error(),
			})
			continue
		}
		holidays = append(holidays, holiday)
	}

	return holidays, nil
}

// FindByDate checks if a specific date is a holiday
func (r *HolidayRepository) FindByDate(ctx context.Context, date time.Time) (*models.Holiday, error) {
	query := `SELECT id, name, date, type, created_at FROM holidays WHERE date = $1`

	holiday := &models.Holiday{}
	var createdAt time.Time
	err := r.db.QueryRowContext(ctx, query, date).Scan(
		&holiday.ID, &holiday.Name, &holiday.Date, &holiday.Type, &createdAt,
	)

	if err == sql.ErrNoRows {
		return nil, nil // Not a holiday
	}
	if err != nil {
		r.log.Error("Database error finding holiday by date", map[string]interface{}{
			"date":  date,
			"error": err.Error(),
		})
		return nil, fmt.Errorf("database error: %w", err)
	}

	return holiday, nil
}

// Create inserts a new holiday
func (r *HolidayRepository) Create(ctx context.Context, holiday *models.Holiday) error {
	query := `INSERT INTO holidays (name, date, type) 
              VALUES ($1, $2, $3) 
              RETURNING id, created_at`

	var createdAt time.Time
	err := r.db.QueryRowContext(ctx, query,
		holiday.Name, holiday.Date, holiday.Type,
	).Scan(&holiday.ID, &createdAt)

	if err != nil {
		r.log.Error("Database error creating holiday", map[string]interface{}{
			"name":  holiday.Name,
			"error": err.Error(),
		})
		return fmt.Errorf("failed to create holiday: %w", err)
	}

	// Invalidate cache
	if r.cache != nil {
		r.cache.Delete(ctx, "holidays:all")
	}

	r.log.Info("Holiday created", map[string]interface{}{
		"holidayId": holiday.ID,
		"name":      holiday.Name,
	})

	return nil
}

// Delete removes a holiday
func (r *HolidayRepository) Delete(ctx context.Context, id int64) error {
	query := `DELETE FROM holidays WHERE id = $1`

	result, err := r.db.ExecContext(ctx, query, id)
	if err != nil {
		r.log.Error("Database error deleting holiday", map[string]interface{}{
			"holidayId": id,
			"error":     err.Error(),
		})
		return fmt.Errorf("failed to delete holiday: %w", err)
	}

	rowsAffected, _ := result.RowsAffected()
	if rowsAffected == 0 {
		return fmt.Errorf("holiday not found")
	}

	// Invalidate cache
	if r.cache != nil {
		r.cache.Delete(ctx, "holidays:all")
	}

	r.log.Info("Holiday deleted", map[string]interface{}{
		"holidayId": id,
	})

	return nil
}
