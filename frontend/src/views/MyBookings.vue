<template>
  <div class="my-bookings-container">
    <h1>My Bookings</h1>
    
    <div v-if="bookingStore.loading" class="loading">Loading your bookings...</div>
    <div v-else-if="bookingStore.error" class="error-message">{{ bookingStore.error }}</div>
    
    <div v-else-if="bookingStore.myBookings.length === 0" class="no-bookings">
      <p>You haven't made any bookings yet.</p>
      <router-link to="/rooms" class="btn-primary">Browse Rooms</router-link>
    </div>
    
    <div v-else class="bookings-list">
      <div
        v-for="booking in sortedBookings"
        :key="booking.id"
        class="booking-card card"
        :class="{ 'cancelled': booking.status === 'CANCELLED', 'past': isPast(booking.endTime) }"
      >
        <div class="booking-header">
          <h3>{{ booking.title }}</h3>
          <span :class="['status-badge', getStatusClass(booking)]">
            {{ getStatusLabel(booking) }}
          </span>
        </div>
        
        <div class="booking-details">
          <div class="detail-row">
            <span class="icon">📍</span>
            <strong>Room:</strong> {{ booking.roomName }}
          </div>
          <div class="detail-row">
            <span class="icon">📅</span>
            <strong>Date:</strong> {{ formatDate(booking.startTime) }}
          </div>
          <div class="detail-row">
            <span class="icon">🕐</span>
            <strong>Time:</strong> {{ formatTime(booking.startTime) }} - {{ formatTime(booking.endTime) }}
          </div>
          <div v-if="booking.description" class="detail-row description">
            <span class="icon">📝</span>
            <span>{{ booking.description }}</span>
          </div>
        </div>
        
        <div class="booking-actions">
          <button
            v-if="canCancel(booking)"
            @click="handleCancel(booking.id)"
            class="btn-cancel"
            :disabled="cancelling === booking.id"
          >
            {{ cancelling === booking.id ? 'Cancelling...' : 'Cancel Booking' }}
          </button>
          <router-link
            :to="`/rooms/${booking.roomId}`"
            class="btn-secondary"
          >
            View Room
          </router-link>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useBookingStore } from '@/stores/booking'
import type { Booking } from '@/types'

const bookingStore = useBookingStore()
const cancelling = ref<number | null>(null)

onMounted(() => {
  bookingStore.fetchMyBookings()
})

const sortedBookings = computed(() => {
  return [...bookingStore.myBookings].sort((a, b) => {
    return new Date(b.startTime).getTime() - new Date(a.startTime).getTime()
  })
})

const formatDate = (dateString: string) => {
  const date = new Date(dateString)
  return date.toLocaleDateString('en-US', {
    weekday: 'long',
    year: 'numeric',
    month: 'long',
    day: 'numeric'
  })
}

const formatTime = (dateString: string) => {
  const date = new Date(dateString)
  return date.toLocaleTimeString('en-US', {
    hour: '2-digit',
    minute: '2-digit'
  })
}

const isPast = (dateString: string) => {
  return new Date(dateString) < new Date()
}

const canCancel = (booking: Booking) => {
  return booking.status === 'CONFIRMED' && new Date(booking.startTime) > new Date()
}

const getStatusClass = (booking: Booking) => {
  if (booking.status === 'CANCELLED') return 'cancelled'
  if (isPast(booking.endTime)) return 'past'
  if (new Date(booking.startTime) <= new Date() && new Date(booking.endTime) >= new Date()) return 'ongoing'
  return 'upcoming'
}

const getStatusLabel = (booking: Booking) => {
  if (booking.status === 'CANCELLED') return 'Cancelled'
  if (isPast(booking.endTime)) return 'Completed'
  if (new Date(booking.startTime) <= new Date() && new Date(booking.endTime) >= new Date()) return 'Ongoing'
  return 'Upcoming'
}

const handleCancel = async (bookingId: number) => {
  if (!confirm('Are you sure you want to cancel this booking?')) return
  
  cancelling.value = bookingId
  try {
    await bookingStore.cancelBooking(bookingId)
  } finally {
    cancelling.value = null
  }
}
</script>

<style scoped>
.my-bookings-container {
  padding: 2rem 0;
}

h1 {
  margin-bottom: 2rem;
  color: #2c3e50;
}

.no-bookings {
  text-align: center;
  padding: 3rem;
}

.no-bookings p {
  margin-bottom: 1.5rem;
  color: #666;
  font-size: 1.1rem;
}

.bookings-list {
  display: flex;
  flex-direction: column;
  gap: 1.5rem;
}

.booking-card {
  transition: transform 0.3s, box-shadow 0.3s;
}

.booking-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(0,0,0,0.15);
}

.booking-card.cancelled {
  opacity: 0.6;
  border-left: 4px solid #e74c3c;
}

.booking-card.past {
  border-left: 4px solid #95a5a6;
}

.booking-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 1.5rem;
  padding-bottom: 1rem;
  border-bottom: 2px solid #f0f0f0;
}

.booking-header h3 {
  margin: 0;
  color: #2c3e50;
}

.status-badge {
  padding: 0.35rem 0.85rem;
  border-radius: 12px;
  font-size: 0.875rem;
  font-weight: 600;
}

.status-badge.upcoming {
  background-color: #d1ecf1;
  color: #0c5460;
}

.status-badge.ongoing {
  background-color: #fff3cd;
  color: #856404;
}

.status-badge.past {
  background-color: #e2e3e5;
  color: #383d41;
}

.status-badge.cancelled {
  background-color: #f8d7da;
  color: #721c24;
}

.booking-details {
  margin-bottom: 1.5rem;
}

.detail-row {
  display: flex;
  align-items: flex-start;
  gap: 0.5rem;
  margin-bottom: 0.75rem;
  color: #555;
}

.icon {
  font-size: 1.2rem;
}

.detail-row.description {
  margin-top: 1rem;
  padding-top: 1rem;
  border-top: 1px solid #eee;
  color: #666;
}

.booking-actions {
  display: flex;
  gap: 1rem;
  margin-top: 1rem;
}

.btn-primary, .btn-secondary, .btn-cancel {
  padding: 0.75rem 1.5rem;
  border: none;
  border-radius: 4px;
  font-size: 1rem;
  cursor: pointer;
  text-decoration: none;
  display: inline-block;
  text-align: center;
  transition: background-color 0.3s;
}

.btn-primary {
  background-color: #3498db;
  color: white;
}

.btn-primary:hover {
  background-color: #2980b9;
}

.btn-secondary {
  background-color: #6c757d;
  color: white;
}

.btn-secondary:hover {
  background-color: #5a6268;
}

.btn-cancel {
  background-color: #e74c3c;
  color: white;
  flex: 1;
}

.btn-cancel:hover:not(:disabled) {
  background-color: #c0392b;
}

.btn-cancel:disabled {
  background-color: #95a5a6;
  cursor: not-allowed;
}

@media (max-width: 768px) {
  .booking-actions {
    flex-direction: column;
  }
}
</style>
