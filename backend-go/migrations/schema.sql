-- Meeting Room Booking Database Schema
-- PostgreSQL implementation matching Spring Boot JPA entities

-- Enable UUID extension (if needed)
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- Users table (matches User entity)
CREATE TABLE IF NOT EXISTS users (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(255) NOT NULL UNIQUE,
    email VARCHAR(255) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL, -- BCrypt hashed
    roles VARCHAR(255) NOT NULL DEFAULT 'ROLE_USER',
    enabled BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Index on username for faster lookups
CREATE INDEX IF NOT EXISTS idx_users_username ON users(username);
CREATE INDEX IF NOT EXISTS idx_users_email ON users(email);

-- Meeting rooms table (matches MeetingRoom entity)
CREATE TABLE IF NOT EXISTS meeting_rooms (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL UNIQUE,
    capacity INTEGER NOT NULL,
    location VARCHAR(255) NOT NULL,
    available BOOLEAN NOT NULL DEFAULT true,
    features TEXT, -- Comma-separated string
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Index on availability for faster queries
CREATE INDEX IF NOT EXISTS idx_rooms_available ON meeting_rooms(available);

-- Bookings table (matches Booking entity with optimistic locking)
CREATE TABLE IF NOT EXISTS bookings (
    id BIGSERIAL PRIMARY KEY,
    room_id BIGINT NOT NULL REFERENCES meeting_rooms(id) ON DELETE CASCADE,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    start_time TIMESTAMP NOT NULL,
    end_time TIMESTAMP NOT NULL,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    status VARCHAR(50) NOT NULL DEFAULT 'CONFIRMED', -- CONFIRMED, CANCELLED
    version INTEGER NOT NULL DEFAULT 0, -- For optimistic locking
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_booking_times CHECK (end_time > start_time)
);

-- Composite indexes matching Spring Boot @Table(indexes = ...)
-- Index for finding bookings by room and time range
CREATE INDEX IF NOT EXISTS idx_room_time ON bookings(room_id, start_time, end_time);

-- Index for finding user bookings
CREATE INDEX IF NOT EXISTS idx_user_booking ON bookings(user_id, start_time);

-- Index for status queries
CREATE INDEX IF NOT EXISTS idx_booking_status ON bookings(status);

-- Holidays table
CREATE TABLE IF NOT EXISTS holidays (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    date DATE NOT NULL UNIQUE,
    type VARCHAR(50) NOT NULL DEFAULT 'PUBLIC', -- PUBLIC, COMPANY
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Index on date for faster lookups
CREATE INDEX IF NOT EXISTS idx_holidays_date ON holidays(date);

-- Function to update updated_at timestamp automatically
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ language 'plpgsql';

-- Triggers for automatic updated_at updates
CREATE TRIGGER update_users_updated_at BEFORE UPDATE ON users
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_rooms_updated_at BEFORE UPDATE ON meeting_rooms
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_bookings_updated_at BEFORE UPDATE ON bookings
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- Insert demo data
-- Admin user (password: admin123 -> SHA256 -> BCrypt)
-- SHA-256 of "admin123" = "240be518fabd2724ddb6f04eeb1da5967448d7e831c08c8fa822809f74c720a9"
-- Then BCrypt with strength 12
INSERT INTO users (username, email, password, roles, enabled) VALUES
('admin', 'admin@example.com', '$2a$12$YourBCryptHashHere', 'ROLE_USER,ROLE_ADMIN', true),
('user1', 'user1@example.com', '$2a$12$YourBCryptHashHere', 'ROLE_USER', true),
('user2', 'user2@example.com', '$2a$12$YourBCryptHashHere', 'ROLE_USER', true)
ON CONFLICT (username) DO NOTHING;

-- Meeting rooms
INSERT INTO meeting_rooms (name, capacity, location, available, features) VALUES
('Conference Room A', 10, 'Floor 1', true, 'Projector,Whiteboard,Video Conference'),
('Conference Room B', 8, 'Floor 1', true, 'Whiteboard,TV'),
('Meeting Room 101', 4, 'Floor 2', true, 'Whiteboard'),
('Meeting Room 102', 6, 'Floor 2', true, 'Projector,Whiteboard'),
('Board Room', 20, 'Floor 3', true, 'Projector,Video Conference,Whiteboard,TV'),
('Small Room 1', 2, 'Floor 1', true, 'Whiteboard'),
('Small Room 2', 2, 'Floor 1', true, 'Whiteboard')
ON CONFLICT (name) DO NOTHING;

-- Holidays
INSERT INTO holidays (name, date, type) VALUES
('New Year', '2024-01-01', 'PUBLIC'),
('Independence Day', '2024-08-17', 'PUBLIC'),
('Christmas', '2024-12-25', 'PUBLIC'),
('Company Anniversary', '2024-06-15', 'COMPANY')
ON CONFLICT (date) DO NOTHING;

-- Sample bookings (use actual user and room IDs)
-- These will be inserted dynamically by the application or manually adjusted
