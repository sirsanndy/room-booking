package config

import (
	"fmt"
	"os"
	"strconv"
	"strings"
	"time"
)

// Config holds all application configuration
type Config struct {
	Server    ServerConfig
	Database  DatabaseConfig
	Redis     RedisConfig
	JWT       JWTConfig
	CORS      CORSConfig
	RateLimit RateLimitConfig
	Log       LogConfig
	Metrics   MetricsConfig
}

type ServerConfig struct {
	Port string
	Env  string
}

type DatabaseConfig struct {
	Host            string
	Port            string
	User            string
	Password        string
	Database        string
	SSLMode         string
	MaxOpenConns    int
	MaxIdleConns    int
	ConnMaxLifetime time.Duration
	ConnMaxIdleTime time.Duration
}

type RedisConfig struct {
	Host       string
	Port       string
	Password   string
	DB         int
	PoolSize   int
	DefaultTTL time.Duration
	RoomTTL    time.Duration
	UserTTL    time.Duration
	HolidayTTL time.Duration
}

type JWTConfig struct {
	Secret         string
	ExpirationTime time.Duration
	Issuer         string
}

type CORSConfig struct {
	AllowedOrigins []string
	AllowedMethods []string
	AllowedHeaders []string
	MaxAge         int
}

type RateLimitConfig struct {
	RequestsPerSecond float64
	Burst             int
}

type LogConfig struct {
	Level      string
	Format     string
	Output     string
	MaxSize    int // megabytes
	MaxBackups int
	MaxAge     int // days
	Compress   bool
}

type MetricsConfig struct {
	Enabled bool
	Port    string
	Path    string
}

// Load loads configuration from environment variables
func Load() (*Config, error) {
	cfg := &Config{
		Server: ServerConfig{
			Port: getEnv("SERVER_PORT", "8080"),
			Env:  getEnv("SERVER_ENV", "development"),
		},
		Database: DatabaseConfig{
			Host:            getEnv("DB_HOST", "localhost"),
			Port:            getEnv("DB_PORT", "5432"),
			User:            getEnv("DB_USER", "postgres"),
			Password:        getEnv("DB_PASSWORD", "postgres"),
			Database:        getEnv("DB_NAME", "meetingroom_db"),
			SSLMode:         getEnv("DB_SSL_MODE", "disable"),
			MaxOpenConns:    getEnvAsInt("DB_MAX_OPEN_CONNS", 20),
			MaxIdleConns:    getEnvAsInt("DB_MAX_IDLE_CONNS", 5),
			ConnMaxLifetime: getEnvAsDuration("DB_CONN_MAX_LIFETIME", 30*time.Minute),
			ConnMaxIdleTime: getEnvAsDuration("DB_CONN_MAX_IDLE_TIME", 5*time.Minute),
		},
		Redis: RedisConfig{
			Host:       getEnv("REDIS_HOST", "localhost"),
			Port:       getEnv("REDIS_PORT", "6379"),
			Password:   getEnv("REDIS_PASSWORD", ""),
			DB:         getEnvAsInt("REDIS_DB", 0),
			PoolSize:   getEnvAsInt("REDIS_POOL_SIZE", 10),
			DefaultTTL: getEnvAsDuration("REDIS_DEFAULT_TTL", 1*time.Hour),
			RoomTTL:    getEnvAsDuration("REDIS_ROOM_TTL", 1*time.Hour),
			UserTTL:    getEnvAsDuration("REDIS_USER_TTL", 2*time.Hour),
			HolidayTTL: getEnvAsDuration("REDIS_HOLIDAY_TTL", 24*time.Hour),
		},
		JWT: JWTConfig{
			Secret:         getEnv("JWT_SECRET", "your-secret-key-change-this-in-production"),
			ExpirationTime: getEnvAsDuration("JWT_EXPIRATION", 24*time.Hour),
			Issuer:         getEnv("JWT_ISSUER", "meeting-room-booking"),
		},
		CORS: CORSConfig{
			AllowedOrigins: getEnvAsSlice("CORS_ALLOWED_ORIGINS", []string{"http://localhost:5173", "http://localhost:3000", "http://localhost:4173"}),
			AllowedMethods: []string{"GET", "POST", "PUT", "DELETE", "OPTIONS"},
			AllowedHeaders: []string{"Content-Type", "Authorization"},
			MaxAge:         3600,
		},
		RateLimit: RateLimitConfig{
			RequestsPerSecond: getEnvAsFloat("RATE_LIMIT_RPS", 10.0),
			Burst:             getEnvAsInt("RATE_LIMIT_BURST", 20),
		},
		Log: LogConfig{
			Level:      getEnv("LOG_LEVEL", "info"),
			Format:     getEnv("LOG_FORMAT", "json"),
			Output:     getEnv("LOG_OUTPUT", "stdout"),
			MaxSize:    getEnvAsInt("LOG_MAX_SIZE", 100),
			MaxBackups: getEnvAsInt("LOG_MAX_BACKUPS", 3),
			MaxAge:     getEnvAsInt("LOG_MAX_AGE", 28),
			Compress:   getEnvAsBool("LOG_COMPRESS", true),
		},
		Metrics: MetricsConfig{
			Enabled: getEnvAsBool("METRICS_ENABLED", true),
			Port:    getEnv("METRICS_PORT", "9090"),
			Path:    getEnv("METRICS_PATH", "/metrics"),
		},
	}

	// Validate required fields
	if cfg.JWT.Secret == "your-secret-key-change-this-in-production" && cfg.Server.Env == "production" {
		return nil, fmt.Errorf("JWT_SECRET must be set in production environment")
	}

	return cfg, nil
}

// Helper functions for environment variable parsing
func getEnv(key, defaultValue string) string {
	if value := os.Getenv(key); value != "" {
		return value
	}
	return defaultValue
}

func getEnvAsInt(key string, defaultValue int) int {
	valueStr := os.Getenv(key)
	if value, err := strconv.Atoi(valueStr); err == nil {
		return value
	}
	return defaultValue
}

func getEnvAsFloat(key string, defaultValue float64) float64 {
	valueStr := os.Getenv(key)
	if value, err := strconv.ParseFloat(valueStr, 64); err == nil {
		return value
	}
	return defaultValue
}

func getEnvAsBool(key string, defaultValue bool) bool {
	valueStr := os.Getenv(key)
	if value, err := strconv.ParseBool(valueStr); err == nil {
		return value
	}
	return defaultValue
}

func getEnvAsDuration(key string, defaultValue time.Duration) time.Duration {
	valueStr := os.Getenv(key)
	if value, err := time.ParseDuration(valueStr); err == nil {
		return value
	}
	return defaultValue
}

func getEnvAsSlice(key string, defaultValue []string) []string {
	valueStr := os.Getenv(key)
	if valueStr == "" {
		return defaultValue
	}
	return strings.Split(valueStr, ",")
}
