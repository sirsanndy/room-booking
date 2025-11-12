package config

import (
	"database/sql"
	"fmt"
	"time"

	_ "github.com/lib/pq"
)

// NewDatabase creates a new PostgreSQL database connection pool
func NewDatabase(cfg DatabaseConfig) (*sql.DB, error) {
	// Connection string format: "host=localhost port=5432 user=postgres password=postgres dbname=meetingroom_db sslmode=disable"
	connStr := fmt.Sprintf("host=%s port=%s user=%s password=%s dbname=%s sslmode=%s",
		cfg.Host, cfg.Port, cfg.User, cfg.Password, cfg.Database, cfg.SSLMode)

	db, err := sql.Open("postgres", connStr)
	if err != nil {
		return nil, fmt.Errorf("failed to open database: %w", err)
	}

	// Configure connection pool (equivalent to HikariCP settings)
	db.SetMaxOpenConns(cfg.MaxOpenConns)
	db.SetMaxIdleConns(cfg.MaxIdleConns)
	db.SetConnMaxLifetime(cfg.ConnMaxLifetime)
	db.SetConnMaxIdleTime(cfg.ConnMaxIdleTime)

	// Verify connection
	ctx, cancel := newTimeoutContext(5 * time.Second)
	defer cancel()

	if err := db.PingContext(ctx); err != nil {
		return nil, fmt.Errorf("failed to ping database: %w", err)
	}

	return db, nil
}
