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

type UserRepository struct {
	db    *sql.DB
	cache *config.RedisCache
	log   *logger.Logger
}

func NewUserRepository(db *sql.DB, cache *config.RedisCache, log *logger.Logger) *UserRepository {
	return &UserRepository{
		db:    db,
		cache: cache,
		log:   log,
	}
}

// FindByID retrieves a user by ID with caching
func (r *UserRepository) FindByID(ctx context.Context, id int64) (*models.User, error) {
	// Try cache first
	cacheKey := fmt.Sprintf("user:%d", id)
	if r.cache != nil {
		cached, err := r.cache.Get(ctx, cacheKey)
		if err == nil {
			var user models.User
			if err := json.Unmarshal([]byte(cached), &user); err == nil {
				r.log.Debug("User cache hit", map[string]interface{}{"userId": id})
				return &user, nil
			}
		}
	}

	// Cache miss - query database
	query := `SELECT id, full_name, username, email, password, enabled, created_at, updated_at 
              FROM users WHERE id = $1`

	user := &models.User{}
	err := r.db.QueryRowContext(ctx, query, id).Scan(
		&user.ID, &user.FullName, &user.Username, &user.Email, &user.Password, &user.Enabled, &user.CreatedAt, &user.UpdatedAt,
	)

	if err == sql.ErrNoRows {
		return nil, fmt.Errorf("user not found")
	}
	if err != nil {
		r.log.Error("Database error finding user by ID", map[string]interface{}{
			"userId": id,
			"error":  err.Error(),
		})
		return nil, fmt.Errorf("database error: %w", err)
	}

	// Store in cache
	if r.cache != nil {
		jsonData, _ := json.Marshal(user)
		r.cache.Set(ctx, cacheKey, jsonData, r.cache.UserTTL)
	}

	return user, nil
}

// FindByUsername retrieves a user by username
func (r *UserRepository) FindByUsername(ctx context.Context, username string) (*models.User, error) {
	query := `SELECT id, full_name, username, email, password, enabled, created_at, updated_at 
              FROM users WHERE username = $1`

	user := &models.User{}
	err := r.db.QueryRowContext(ctx, query, username).Scan(
		&user.ID, &user.FullName, &user.Username, &user.Email, &user.Password, &user.Enabled, &user.CreatedAt, &user.UpdatedAt,
	)

	if err == sql.ErrNoRows {
		return nil, fmt.Errorf("user not found")
	}
	if err != nil {
		r.log.Error("Database error finding user by username", map[string]interface{}{
			"username": username,
			"error":    err.Error(),
		})
		return nil, fmt.Errorf("database error: %w", err)
	}

	return user, nil
}

// FindByEmail retrieves a user by email
func (r *UserRepository) FindByEmail(ctx context.Context, email string) (*models.User, error) {
	query := `SELECT id, full_name, username, email, password, enabled, created_at, updated_at 
              FROM users WHERE email = $1`

	user := &models.User{}
	err := r.db.QueryRowContext(ctx, query, email).Scan(
		&user.ID, &user.Username, &user.Email, &user.Password, &user.Enabled, &user.CreatedAt, &user.UpdatedAt,
	)

	if err == sql.ErrNoRows {
		return nil, fmt.Errorf("user not found")
	}
	if err != nil {
		r.log.Error("Database error finding user by email", map[string]interface{}{
			"email": email,
			"error": err.Error(),
		})
		return nil, fmt.Errorf("database error: %w", err)
	}

	return user, nil
}

// Create inserts a new user
func (r *UserRepository) Create(ctx context.Context, user *models.User) error {
	query := `INSERT INTO users (full_name, username, email, password, enabled, created_at, updated_at) 
              VALUES ($1, $2, $3, $4, $5, $6, $7) 
              RETURNING id, created_at, updated_at`

	err := r.db.QueryRowContext(ctx, query,
		user.FullName, user.Username, user.Email, user.Password, user.Enabled, time.Now().UTC(), time.Now().UTC(),
	).Scan(&user.ID, &user.CreatedAt, &user.UpdatedAt)

	if err != nil {
		r.log.Error("Database error creating user", map[string]interface{}{
			"username": user.Username,
			"error":    err.Error(),
		})
		return fmt.Errorf("failed to create user: %w", err)
	}

	r.log.Info("User created", map[string]interface{}{
		"userId":   user.ID,
		"username": user.Username,
	})

	return nil
}

// Update updates an existing user
func (r *UserRepository) Update(ctx context.Context, user *models.User) error {
	query := `UPDATE users 
              SET username = $1, email = $2, password = $3, enabled = $4, full_name = $5 
              WHERE id = $6`

	result, err := r.db.ExecContext(ctx, query,
		user.Username, user.Email, user.Password, user.Enabled, user.FullName, user.ID,
	)

	if err != nil {
		r.log.Error("Database error updating user", map[string]interface{}{
			"userId": user.ID,
			"error":  err.Error(),
		})
		return fmt.Errorf("failed to update user: %w", err)
	}

	rowsAffected, _ := result.RowsAffected()
	if rowsAffected == 0 {
		return fmt.Errorf("user not found")
	}

	// Invalidate cache
	if r.cache != nil {
		cacheKey := fmt.Sprintf("user:%d", user.ID)
		r.cache.Delete(ctx, cacheKey)
	}

	r.log.Info("User updated", map[string]interface{}{
		"userId": user.ID,
	})

	return nil
}

// Delete soft deletes a user by setting enabled to false
func (r *UserRepository) Delete(ctx context.Context, id int64) error {
	query := `UPDATE users SET enabled = false WHERE id = $1`

	result, err := r.db.ExecContext(ctx, query, id)
	if err != nil {
		r.log.Error("Database error deleting user", map[string]interface{}{
			"userId": id,
			"error":  err.Error(),
		})
		return fmt.Errorf("failed to delete user: %w", err)
	}

	rowsAffected, _ := result.RowsAffected()
	if rowsAffected == 0 {
		return fmt.Errorf("user not found")
	}

	// Invalidate cache
	if r.cache != nil {
		cacheKey := fmt.Sprintf("user:%d", id)
		r.cache.Delete(ctx, cacheKey)
	}

	r.log.Info("User deleted", map[string]interface{}{
		"userId": id,
	})

	return nil
}

// FindByIDs retrieves multiple users by IDs (for N+1 prevention)
func (r *UserRepository) FindByIDs(ctx context.Context, ids []int64) (map[int64]*models.User, error) {
	if len(ids) == 0 {
		return make(map[int64]*models.User), nil
	}

	query := `SELECT id, full_name, username, email, password, enabled, created_at, updated_at 
              FROM users WHERE id = ANY($1)`

	rows, err := r.db.QueryContext(ctx, query, ids)
	if err != nil {
		r.log.Error("Database error finding users by IDs", map[string]interface{}{
			"error": err.Error(),
		})
		return nil, fmt.Errorf("database error: %w", err)
	}
	defer rows.Close()

	users := make(map[int64]*models.User)
	for rows.Next() {
		user := &models.User{}
		err := rows.Scan(
			&user.ID, &user.FullName, &user.Username, &user.Email, &user.Password, &user.Enabled, &user.CreatedAt, &user.UpdatedAt,
		)
		if err != nil {
			r.log.Error("Error scanning user row", map[string]interface{}{
				"error": err.Error(),
			})
			continue
		}
		users[user.ID] = user
	}

	return users, nil
}
