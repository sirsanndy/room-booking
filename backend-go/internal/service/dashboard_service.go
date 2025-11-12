package service

import (
	"context"
	"sync"

	"meetingroom/internal/domain/models"
	"meetingroom/internal/logger"
	"meetingroom/internal/repository"
)

type DashboardService struct {
	bookingRepo *repository.BookingRepository
	roomRepo    *repository.RoomRepository
	holidayRepo *repository.HolidayRepository
	log         *logger.Logger
}

func NewDashboardService(
	bookingRepo *repository.BookingRepository,
	roomRepo *repository.RoomRepository,
	holidayRepo *repository.HolidayRepository,
	log *logger.Logger,
) *DashboardService {
	return &DashboardService{
		bookingRepo: bookingRepo,
		roomRepo:    roomRepo,
		holidayRepo: holidayRepo,
		log:         log,
	}
}

// GetStats retrieves dashboard statistics
func (s *DashboardService) GetStats(ctx context.Context) (*models.DashboardStats, error) {
	stats := &models.DashboardStats{}

	// Use WaitGroup to run queries in parallel
	var wg sync.WaitGroup
	var mu sync.Mutex
	errors := make([]error, 0)

	// Total bookings
	wg.Add(1)
	go func() {
		defer wg.Done()
		count, err := s.bookingRepo.CountTotal(ctx)
		if err != nil {
			mu.Lock()
			errors = append(errors, err)
			mu.Unlock()
			return
		}
		mu.Lock()
		stats.TotalBookings = count
		mu.Unlock()
	}()

	// Active bookings
	wg.Add(1)
	go func() {
		defer wg.Done()
		count, err := s.bookingRepo.CountActive(ctx)
		if err != nil {
			mu.Lock()
			errors = append(errors, err)
			mu.Unlock()
			return
		}
		mu.Lock()
		stats.ActiveBookings = count
		mu.Unlock()
	}()

	// Total rooms
	wg.Add(1)
	go func() {
		defer wg.Done()
		rooms, err := s.roomRepo.FindAll(ctx)
		if err != nil {
			mu.Lock()
			errors = append(errors, err)
			mu.Unlock()
			return
		}
		mu.Lock()
		stats.TotalRooms = len(rooms)
		mu.Unlock()
	}()

	// Available rooms
	wg.Add(1)
	go func() {
		defer wg.Done()
		rooms, err := s.roomRepo.FindAvailable(ctx)
		if err != nil {
			mu.Lock()
			errors = append(errors, err)
			mu.Unlock()
			return
		}
		mu.Lock()
		stats.AvailableRooms = len(rooms)
		mu.Unlock()
	}()

	// Bookings by room
	wg.Add(1)
	go func() {
		defer wg.Done()
		counts, err := s.bookingRepo.CountByRoom(ctx)
		if err != nil {
			mu.Lock()
			errors = append(errors, err)
			mu.Unlock()
			return
		}
		mu.Lock()
		stats.BookingsByRoom = counts
		mu.Unlock()
	}()

	// Bookings by status
	wg.Add(1)
	go func() {
		defer wg.Done()
		counts, err := s.bookingRepo.CountByStatus(ctx)
		if err != nil {
			mu.Lock()
			errors = append(errors, err)
			mu.Unlock()
			return
		}
		mu.Lock()
		stats.BookingsByStatus = counts
		mu.Unlock()
	}()

	// Upcoming bookings
	wg.Add(1)
	go func() {
		defer wg.Done()
		bookings, err := s.bookingRepo.FindUpcoming(ctx, 10)
		if err != nil {
			mu.Lock()
			errors = append(errors, err)
			mu.Unlock()
			return
		}
		mu.Lock()
		// Convert []*BookingWithDetails to []BookingWithDetails
		result := make([]models.BookingWithDetails, 0, len(bookings))
		for _, b := range bookings {
			if b != nil {
				result = append(result, *b)
			}
		}
		stats.UpcomingBookings = result
		mu.Unlock()
	}()

	// Recent bookings
	wg.Add(1)
	go func() {
		defer wg.Done()
		bookings, err := s.bookingRepo.FindRecent(ctx, 10)
		if err != nil {
			mu.Lock()
			errors = append(errors, err)
			mu.Unlock()
			return
		}
		mu.Lock()
		// Convert []*BookingWithDetails to []BookingWithDetails
		result := make([]models.BookingWithDetails, 0, len(bookings))
		for _, b := range bookings {
			if b != nil {
				result = append(result, *b)
			}
		}
		stats.RecentBookings = result
		mu.Unlock()
	}()

	// Upcoming holidays
	wg.Add(1)
	go func() {
		defer wg.Done()
		holidays, err := s.holidayRepo.FindUpcoming(ctx, 5)
		if err != nil {
			mu.Lock()
			errors = append(errors, err)
			mu.Unlock()
			return
		}
		mu.Lock()
		// Convert []*Holiday to []Holiday
		result := make([]models.Holiday, 0, len(holidays))
		for _, h := range holidays {
			if h != nil {
				result = append(result, *h)
			}
		}
		stats.UpcomingHolidays = result
		mu.Unlock()
	}()

	// Wait for all goroutines to complete
	wg.Wait()

	// Check if there were any errors
	if len(errors) > 0 {
		s.log.Error("Failed to get dashboard stats", map[string]interface{}{
			"errors": errors,
		})
		return nil, errors[0]
	}

	s.log.Info("Dashboard stats retrieved successfully", nil)

	return stats, nil
}
