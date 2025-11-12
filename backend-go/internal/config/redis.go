package config

import (
	"context"
	"fmt"
	"time"

	"github.com/redis/go-redis/v9"
)

// RedisCache wraps redis.Client with additional functionality
type RedisCache struct {
	Client     *redis.Client
	DefaultTTL time.Duration
	RoomTTL    time.Duration
	UserTTL    time.Duration
	HolidayTTL time.Duration
}

// NewRedisCache creates a new Redis client
func NewRedisCache(cfg RedisConfig) (*RedisCache, error) {
	client := redis.NewClient(&redis.Options{
		Addr:     fmt.Sprintf("%s:%s", cfg.Host, cfg.Port),
		Password: cfg.Password,
		DB:       cfg.DB,
		PoolSize: cfg.PoolSize,
	})

	// Verify connection
	ctx, cancel := newTimeoutContext(5 * time.Second)
	defer cancel()

	if err := client.Ping(ctx).Err(); err != nil {
		return nil, fmt.Errorf("failed to connect to Redis: %w", err)
	}

	return &RedisCache{
		Client:     client,
		DefaultTTL: cfg.DefaultTTL,
		RoomTTL:    cfg.RoomTTL,
		UserTTL:    cfg.UserTTL,
		HolidayTTL: cfg.HolidayTTL,
	}, nil
}

// Close closes the Redis connection
func (rc *RedisCache) Close() error {
	return rc.Client.Close()
}

// Get retrieves a value from cache
func (rc *RedisCache) Get(ctx context.Context, key string) (string, error) {
	return rc.Client.Get(ctx, key).Result()
}

// Set stores a value in cache with default TTL
func (rc *RedisCache) Set(ctx context.Context, key string, value interface{}, ttl time.Duration) error {
	if ttl == 0 {
		ttl = rc.DefaultTTL
	}
	return rc.Client.Set(ctx, key, value, ttl).Err()
}

// Delete removes a value from cache
func (rc *RedisCache) Delete(ctx context.Context, keys ...string) error {
	return rc.Client.Del(ctx, keys...).Err()
}

// DeletePattern deletes all keys matching a pattern
func (rc *RedisCache) DeletePattern(ctx context.Context, pattern string) error {
	iter := rc.Client.Scan(ctx, 0, pattern, 0).Iterator()
	var keys []string
	for iter.Next(ctx) {
		keys = append(keys, iter.Val())
	}
	if err := iter.Err(); err != nil {
		return err
	}
	if len(keys) > 0 {
		return rc.Client.Del(ctx, keys...).Err()
	}
	return nil
}

// Exists checks if a key exists
func (rc *RedisCache) Exists(ctx context.Context, key string) (bool, error) {
	result, err := rc.Client.Exists(ctx, key).Result()
	return result > 0, err
}

// Helper function to create context with timeout
func newTimeoutContext(timeout time.Duration) (context.Context, context.CancelFunc) {
	return context.WithTimeout(context.Background(), timeout)
}
