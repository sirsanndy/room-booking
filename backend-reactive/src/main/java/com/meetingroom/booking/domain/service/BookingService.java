package com.meetingroom.booking.domain.service;

import com.meetingroom.booking.domain.model.Booking;
import com.meetingroom.booking.domain.model.MeetingRoom;
import com.meetingroom.booking.domain.model.User;
import com.meetingroom.booking.domain.port.in.BookingUseCase;
import com.meetingroom.booking.domain.port.out.BookingRepository;
import com.meetingroom.booking.domain.port.out.CachePort;
import com.meetingroom.booking.domain.port.out.MeetingRoomRepository;
import com.meetingroom.booking.domain.port.out.RateLimiterPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Service
public class BookingService implements BookingUseCase {
    private static final Logger LOG = LoggerFactory.getLogger(BookingService.class);
    
    private static final LocalTime EARLIEST_BOOKING_TIME = LocalTime.of(7, 0);
    private static final LocalTime LATEST_BOOKING_TIME = LocalTime.of(22, 0);
    private static final int MAX_BOOKING_HOURS_PER_DAY = 9;
    private static final long BOOKING_CACHE_TTL = 1800000; // 30 minutes
    
    private final BookingRepository bookingRepository;
    private final MeetingRoomRepository meetingRoomRepository;
    private final HolidayService holidayService;
    private final RateLimiterPort rateLimiterPort;
    private final CachePort cachePort;
    
    public BookingService(BookingRepository bookingRepository,
                         MeetingRoomRepository meetingRoomRepository,
                         HolidayService holidayService,
                         RateLimiterPort rateLimiterPort,
                         CachePort cachePort) {
        this.bookingRepository = bookingRepository;
        this.meetingRoomRepository = meetingRoomRepository;
        this.holidayService = holidayService;
        this.rateLimiterPort = rateLimiterPort;
        this.cachePort = cachePort;
    }
    
    @Override
    public Mono<Booking> createBooking(Long roomId, Long userId, LocalDateTime startTime, 
                                      LocalDateTime endTime, String title, String description,
                                      String username) {
        LOG.info("Creating booking - user: {}, room: {}, start: {}, end: {}", 
            username, roomId, startTime, endTime);
        
        String rateLimitKey = username + ":create_booking";
        
        return rateLimiterPort.allowRequest(rateLimitKey)
            .flatMap(allowed -> {
                if (!allowed) {
                    return rateLimiterPort.getTimeUntilRefill(rateLimitKey)
                        .flatMap(retryAfter -> {
                            LOG.warn("Rate limit exceeded for user {}", username);
                            return Mono.error(new RateLimitExceededException(
                                "Too many booking requests. Please try again in " + retryAfter + " seconds.",
                                retryAfter
                            ));
                        });
                }
                
                return validateBookingTimes(startTime, endTime)
                    .then(validateBookingTimeRestrictions(startTime, endTime))
                    .then(validateDailyBookingHoursLimit(userId, startTime, endTime))
                    .then(validateUserNotDoubleBooked(userId, startTime, endTime))
                    .then(meetingRoomRepository.findById(roomId))
                    .switchIfEmpty(Mono.error(new RuntimeException("Meeting room not found")))
                    .flatMap(room -> validateRoomAvailable(room)
                        .then(checkOverlappingBookings(roomId, startTime, endTime))
                        .then(createAndSaveBooking(roomId, userId, startTime, endTime, title, description))
                    )
                    .flatMap(savedBooking -> clearBookingCaches().thenReturn(savedBooking))
                    .doOnSuccess(booking -> LOG.info("Booking created successfully - id: {}", booking.getId()));
            });
    }
    
    @Override
    public Flux<Booking> getUpcomingBookingsByRoom(Long roomId) {
        String cacheKey = "roomBookings:" + roomId;
        
        return cachePort.get(cacheKey, Booking[].class)
            .flatMapMany(Flux::fromArray)
            .switchIfEmpty(
                bookingRepository.findUpcomingBookingsByRoom(roomId, LocalDateTime.now())
                    .collectList()
                    .flatMap(bookingList -> {
                        Booking[] bookingsArray = new Booking[bookingList.size()];
                        if (!bookingList.isEmpty()) {
                            bookingsArray = bookingList.toArray(new Booking[0]);
                            return cachePort.set(cacheKey, bookingsArray, BOOKING_CACHE_TTL)
                                .thenReturn(bookingsArray);
                        }
                        return Mono.just(bookingsArray);
                    })
                    .flatMapMany(Flux::fromArray)
            )
            .doOnSubscribe(s -> LOG.info("Fetching upcoming bookings for room: {}", roomId));
    }
    
    @Override
    public Flux<Booking> getUserBookings(Long userId) {
        String cacheKey = "userBookings:" + userId;
        
        return cachePort.get(cacheKey, Booking[].class)
            .flatMapMany(bookings -> Flux.fromArray(bookings))
            .switchIfEmpty(
                bookingRepository.findBookingsByUser(userId)
                    .collectList()
                    .flatMap(bookingList -> {
                        if (!bookingList.isEmpty()) {
                            Booking[] bookingsArray = bookingList.toArray(new Booking[0]);
                            return cachePort.set(cacheKey, bookingsArray, BOOKING_CACHE_TTL)
                                .thenReturn(bookingsArray);
                        }
                        return Mono.just(new Booking[0]);
                    })
                    .flatMapMany(Flux::fromArray)
            )
            .doOnSubscribe(s -> LOG.info("Fetching bookings for user: {}", userId));
    }
    
    @Override
    public Flux<Booking> getAllUpcomingBookings() {
        String cacheKey = "upcomingBookings";
        
        return cachePort.get(cacheKey, Booking[].class)
            .flatMapMany(bookings -> Flux.fromArray(bookings))
            .switchIfEmpty(
                bookingRepository.findAllUpcomingBookings(LocalDateTime.now())
                    .collectList()
                    .flatMap(bookingList -> {
                        if (!bookingList.isEmpty()) {
                            Booking[] bookingsArray = bookingList.toArray(new Booking[0]);
                            return cachePort.set(cacheKey, bookingsArray, BOOKING_CACHE_TTL)
                                .thenReturn(bookingsArray);
                        }
                        return Mono.just(new Booking[0]);
                    })
                    .flatMapMany(Flux::fromArray)
            )
            .doOnSubscribe(s -> LOG.info("Fetching all upcoming bookings"));
    }
    
    @Override
    public Mono<Void> cancelBooking(Long bookingId, Long userId) {
        LOG.info("Cancelling booking {} for user: {}", bookingId, userId);
        
        return bookingRepository.findById(bookingId)
            .switchIfEmpty(Mono.error(new RuntimeException("Booking not found")))
            .flatMap(booking -> {
                if (!booking.getUserId().equals(userId)) {
                    return Mono.error(new RuntimeException("You can only cancel your own bookings"));
                }
                
                if ("CANCELLED".equals(booking.getStatus())) {
                    return Mono.error(new RuntimeException("Booking is already cancelled"));
                }
                
                booking.setStatus("CANCELLED");
                return bookingRepository.save(booking);
            })
            .flatMap(booking -> clearBookingCaches())
            .doOnSuccess(v -> LOG.info("Booking cancelled successfully: {}", bookingId));
    }
    
    private Mono<Void> validateBookingTimes(LocalDateTime startTime, LocalDateTime endTime) {
        if (endTime.isBefore(startTime)) {
            return Mono.error(new IllegalArgumentException("End time must be after start time"));
        }
        
        if (startTime.isBefore(LocalDateTime.now())) {
            return Mono.error(new IllegalArgumentException("Cannot book in the past"));
        }
        
        return Mono.empty();
    }
    
    private Mono<Void> validateBookingTimeRestrictions(LocalDateTime startTime, LocalDateTime endTime) {
        LOG.debug("Validating time restrictions for booking");
        
        // Check if booking is on weekend
        DayOfWeek startDay = startTime.getDayOfWeek();
        if (startDay == DayOfWeek.SATURDAY || startDay == DayOfWeek.SUNDAY) {
            return Mono.error(new IllegalArgumentException("Bookings are not allowed on weekends"));
        }
        
        // Check if booking is on holiday
        return holidayService.isHoliday(startTime.toLocalDate())
            .flatMap(isHoliday -> {
                if (isHoliday) {
                    return Mono.error(new IllegalArgumentException("Bookings are not allowed on holidays"));
                }
                
                // Check booking time window (7 AM to 10 PM)
                LocalTime startLocalTime = startTime.toLocalTime();
                LocalTime endLocalTime = endTime.toLocalTime();
                
                if (startLocalTime.isBefore(EARLIEST_BOOKING_TIME)) {
                    return Mono.error(new IllegalArgumentException(
                        "Bookings cannot start before " + EARLIEST_BOOKING_TIME));
                }
                
                if (endLocalTime.isAfter(LATEST_BOOKING_TIME)) {
                    return Mono.error(new IllegalArgumentException(
                        "Bookings cannot end after " + LATEST_BOOKING_TIME));
                }
                
                return Mono.empty();
            });
    }
    
    private Mono<Void> validateDailyBookingHoursLimit(Long userId, LocalDateTime startTime, LocalDateTime endTime) {
        LocalDateTime dayStart = startTime.toLocalDate().atStartOfDay();
        LocalDateTime dayEnd = dayStart.plusDays(1);
        
        return bookingRepository.findUserBookingsBetween(userId, dayStart, dayEnd)
            .filter(b -> "CONFIRMED".equals(b.getStatus()))
            .map(booking -> {
                Duration duration = Duration.between(booking.getStartTime(), booking.getEndTime());
                return duration.toHours();
            })
            .reduce(0L, Long::sum)
            .flatMap(totalHours -> {
                Duration newBookingDuration = Duration.between(startTime, endTime);
                long newBookingHours = newBookingDuration.toHours();
                long totalWithNew = totalHours + newBookingHours;
                
                if (totalWithNew > MAX_BOOKING_HOURS_PER_DAY) {
                    return Mono.error(new IllegalArgumentException(
                        "Daily booking limit exceeded. Maximum " + MAX_BOOKING_HOURS_PER_DAY + 
                        " hours per day allowed. You have " + totalHours + " hours booked."));
                }
                
                return Mono.empty();
            });
    }
    
    private Mono<Void> validateUserNotDoubleBooked(Long userId, LocalDateTime startTime, LocalDateTime endTime) {
        return bookingRepository.findUserBookingsBetween(userId, startTime, endTime)
            .filter(b -> "CONFIRMED".equals(b.getStatus()))
            .filter(b -> !(b.getEndTime().isBefore(startTime) || b.getStartTime().isAfter(endTime)))
            .hasElements()
            .flatMap(hasOverlap -> {
                if (hasOverlap) {
                    return Mono.error(new IllegalArgumentException(
                        "You already have a booking that overlaps with this time period"));
                }
                return Mono.empty();
            });
    }
    
    private Mono<Void> validateRoomAvailable(MeetingRoom room) {
        if (!room.getAvailable()) {
            return Mono.error(new RuntimeException("Meeting room is not available"));
        }
        return Mono.empty();
    }
    
    private Mono<Void> checkOverlappingBookings(Long roomId, LocalDateTime startTime, LocalDateTime endTime) {
        return bookingRepository.findOverlappingBookings(roomId, startTime, endTime, "CONFIRMED")
            .hasElements()
            .flatMap(hasOverlap -> {
                if (hasOverlap) {
                    return Mono.error(new RuntimeException("Room is already booked for the selected time period"));
                }
                return Mono.empty();
            });
    }
    
    private Mono<Booking> createAndSaveBooking(Long roomId, Long userId, LocalDateTime startTime, 
                                              LocalDateTime endTime, String title, String description) {
        Booking booking = new Booking();
        booking.setRoomId(roomId);
        booking.setUserId(userId);
        booking.setStartTime(startTime);
        booking.setEndTime(endTime);
        booking.setTitle(title);
        booking.setDescription(description);
        booking.setStatus("CONFIRMED");
        
        return bookingRepository.save(booking);
    }
    
    private Mono<Void> clearBookingCaches() {
        return Mono.when(
            cachePort.delete("upcomingBookings"),
            cachePort.delete("roomBookings:*"),
            cachePort.delete("userBookings:*"),
            cachePort.delete("dashboardData:*")
        );
    }
    
    public static class RateLimitExceededException extends RuntimeException {
        private final long retryAfter;
        
        public RateLimitExceededException(String message, long retryAfter) {
            super(message);
            this.retryAfter = retryAfter;
        }
        
        public long getRetryAfter() {
            return retryAfter;
        }
    }
}
