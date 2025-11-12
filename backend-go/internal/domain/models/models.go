package models

import (
	"time"
)

// User represents a user in the system
// Equivalent to @Entity User in Spring Boot
type User struct {
	ID        int64     `json:"id"`
	FullName  string    `json:"fullName"`
	Username  string    `json:"username"`
	Email     string    `json:"email"`
	Password  string    `json:"-"`     // Never expose password in JSON
	Roles     string    `json:"roles"` // Stored as comma-separated string
	Enabled   bool      `json:"enabled"`
	CreatedAt time.Time `json:"createdAt"`
	UpdatedAt time.Time `json:"updatedAt"`
}

// MeetingRoom represents a meeting room
// Equivalent to @Entity MeetingRoom in Spring Boot
type MeetingRoom struct {
	ID          int64     `json:"id"`
	Name        string    `json:"name"`
	Capacity    int       `json:"capacity"`
	Description string    `json:"description"`
	Facilities  string    `json:"facilities"`
	Location    string    `json:"location"`
	Available   bool      `json:"available"`
	CreatedAt   time.Time `json:"createdAt"`
	UpdatedAt   time.Time `json:"updatedAt"`
	Version     int       `json:"version"`
}

// BookingStatus represents the status of a booking
type BookingStatus string

const (
	BookingStatusConfirmed BookingStatus = "CONFIRMED"
	BookingStatusCancelled BookingStatus = "CANCELLED"
)

// Booking represents a room booking
// Equivalent to @Entity Booking in Spring Boot with @Version for optimistic locking
type Booking struct {
	ID          int64         `json:"id"`
	RoomID      int64         `json:"roomId"`
	UserID      int64         `json:"userId"`
	StartTime   time.Time     `json:"startTime"`
	EndTime     time.Time     `json:"endTime"`
	Title       string        `json:"title"`
	Description string        `json:"description"`
	Status      BookingStatus `json:"status"`
	Version     int           `json:"version"` // For optimistic locking
	CreatedAt   time.Time     `json:"createdAt"`
	UpdatedAt   time.Time     `json:"updatedAt"`
}

// BookingWithDetails includes room and user details
// Equivalent to DTO in Spring Boot
type BookingWithDetails struct {
	Booking
	Room *MeetingRoom `json:"room,omitempty"`
	User *User        `json:"user,omitempty"`
}

// Holiday represents a holiday date
type Holiday struct {
	ID          int64     `json:"id"`
	Name        string    `json:"name"`
	Description string    `json:"description"`
	Date        time.Time `json:"date"`
	CreatedAt   time.Time `json:"createdAt"`
}

// DashboardStats represents dashboard statistics
type DashboardStats struct {
	TotalBookings    int                  `json:"totalBookings"`
	ActiveBookings   int                  `json:"activeBookings"`
	TotalRooms       int                  `json:"totalRooms"`
	AvailableRooms   int                  `json:"availableRooms"`
	BookingsByRoom   map[string]int       `json:"bookingsByRoom"`
	BookingsByStatus map[string]int       `json:"bookingsByStatus"`
	UpcomingBookings []BookingWithDetails `json:"upcomingBookings"`
	RecentBookings   []BookingWithDetails `json:"recentBookings"`
	UpcomingHolidays []Holiday            `json:"upcomingHolidays"`
}
