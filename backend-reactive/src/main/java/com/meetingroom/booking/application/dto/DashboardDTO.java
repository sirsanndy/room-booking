package com.meetingroom.booking.application.dto;

import java.util.List;

public class DashboardDTO {
    private List<CalendarEventDTO> events;
    private DashboardStats stats;
    
    public DashboardDTO() {
    }

    public DashboardDTO(List<CalendarEventDTO> events, DashboardStats stats) {
        this.events = events;
        this.stats = stats;
    }

    public List<CalendarEventDTO> getEvents() {
        return events;
    }

    public void setEvents(List<CalendarEventDTO> events) {
        this.events = events;
    }

    public DashboardStats getStats() {
        return stats;
    }

    public void setStats(DashboardStats stats) {
        this.stats = stats;
    }

    public static class DashboardStats {
        private long totalBookings;
        private long upcomingBookings;
        private long completedBookings;
        private List<String> mostBookedRooms;
        
        public DashboardStats() {
        }

        public DashboardStats(long totalBookings, long upcomingBookings, long completedBookings, List<String> mostBookedRooms) {
            this.totalBookings = totalBookings;
            this.upcomingBookings = upcomingBookings;
            this.completedBookings = completedBookings;
            this.mostBookedRooms = mostBookedRooms;
        }

        public long getTotalBookings() {
            return totalBookings;
        }

        public void setTotalBookings(long totalBookings) {
            this.totalBookings = totalBookings;
        }

        public long getUpcomingBookings() {
            return upcomingBookings;
        }

        public void setUpcomingBookings(long upcomingBookings) {
            this.upcomingBookings = upcomingBookings;
        }

        public long getCompletedBookings() {
            return completedBookings;
        }

        public void setCompletedBookings(long completedBookings) {
            this.completedBookings = completedBookings;
        }

        public List<String> getMostBookedRooms() {
            return mostBookedRooms;
        }

        public void setMostBookedRooms(List<String> mostBookedRooms) {
            this.mostBookedRooms = mostBookedRooms;
        }
    }
}
