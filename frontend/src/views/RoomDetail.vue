<template>
  <div class="room-detail-container">
    <button @click="goBack" class="back-button">← Back to Rooms</button>
    
    <div v-if="roomStore.loading" class="loading">Loading room details...</div>
    <div v-else-if="roomStore.error" class="error-message">{{ roomStore.error }}</div>
    
    <div v-else-if="roomStore.selectedRoom" class="content-grid">
      <div class="room-info card">
        <h1>{{ roomStore.selectedRoom.name }}</h1>
        <div class="info-item">
          <strong>Location:</strong> {{ roomStore.selectedRoom.location }}
        </div>
        <div class="info-item">
          <strong>Capacity:</strong> {{ roomStore.selectedRoom.capacity }} people
        </div>
        <div class="info-item">
          <strong>Facilities:</strong> {{ roomStore.selectedRoom.facilities }}
        </div>
        <div class="info-item">
          <strong>Description:</strong> {{ roomStore.selectedRoom.description }}
        </div>
        <div class="info-item">
          <strong>Status:</strong>
          <span :class="['status-badge', roomStore.selectedRoom.available ? 'available' : 'unavailable']">
            {{ roomStore.selectedRoom.available ? 'Available' : 'Unavailable' }}
          </span>
        </div>
      </div>
      
      <div class="booking-section">
        <div class="card">
          <h2>Book This Room</h2>
          <form @submit.prevent="handleBooking">
            <div class="form-group">
              <label for="title">Meeting Title</label>
              <input
                id="title"
                v-model="bookingForm.title"
                type="text"
                required
                placeholder="e.g., Team Planning Meeting"
              />
            </div>
            
            <div class="form-group">
              <label for="startTime">Start Time</label>
              <input
                id="startTime"
                v-model="bookingForm.startTime"
                type="datetime-local"
                required
                :min="minDateTime"
              />
            </div>
            
            <div class="form-group">
              <label for="endTime">End Time</label>
              <input
                id="endTime"
                v-model="bookingForm.endTime"
                type="datetime-local"
                required
                :min="bookingForm.startTime || minDateTime"
              />
            </div>
            
            <div class="form-group">
              <label for="description">Description (Optional)</label>
              <textarea
                id="description"
                v-model="bookingForm.description"
                rows="3"
                placeholder="Add meeting details..."
              ></textarea>
            </div>
            
            <button type="submit" class="btn-primary" :disabled="bookingStore.loading">
              {{ bookingStore.loading ? 'Booking...' : 'Book Room' }}
            </button>
            
            <p v-if="bookingStore.error" class="error-message">{{ bookingStore.error }}</p>
            <p v-if="successMessage" class="success-message">{{ successMessage }}</p>
          </form>
        </div>
        
        <div class="card upcoming-bookings">
          <h3>Upcoming Bookings</h3>
          <div v-if="loadingBookings" class="loading">Loading bookings...</div>
          <div v-else-if="upcomingBookings.length === 0" class="no-bookings">
            No upcoming bookings for this room
          </div>
          <div v-else class="bookings-list">
            <div
              v-for="booking in upcomingBookings"
              :key="booking.id"
              class="booking-item"
            >
              <div class="booking-title">{{ booking.title }}</div>
              <div class="booking-time">
                {{ formatDateTime(booking.startTime) }} - {{ formatTime(booking.endTime) }}
              </div>
              <div class="booking-user">By: {{ booking.username }}</div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useRoomStore } from '@/stores/room'
import { useBookingStore } from '@/stores/booking'
import type { Booking } from '@/types'

const route = useRoute()
const router = useRouter()
const roomStore = useRoomStore()
const bookingStore = useBookingStore()

const bookingForm = ref({
  title: '',
  startTime: '',
  endTime: '',
  description: ''
})

const successMessage = ref('')
const upcomingBookings = ref<Booking[]>([])
const loadingBookings = ref(false)

const minDateTime = computed(() => {
  const now = new Date()
  now.setMinutes(now.getMinutes() - now.getTimezoneOffset())
  return now.toISOString().slice(0, 16)
})

onMounted(async () => {
  const roomId = Number(route.params.id)
  await roomStore.fetchRoomById(roomId)
  await loadUpcomingBookings(roomId)
})

const loadUpcomingBookings = async (roomId: number) => {
  loadingBookings.value = true
  try {
    upcomingBookings.value = await bookingStore.fetchBookingsByRoom(roomId)
  } finally {
    loadingBookings.value = false
  }
}

const handleBooking = async () => {
  if (!roomStore.selectedRoom) return
  
  successMessage.value = ''
  
  try {
    await bookingStore.createBooking({
      roomId: roomStore.selectedRoom.id,
      startTime: bookingForm.value.startTime + ':00',
      endTime: bookingForm.value.endTime + ':00',
      title: bookingForm.value.title,
      description: bookingForm.value.description
    })
    
    successMessage.value = 'Room booked successfully!'
    bookingForm.value = { title: '', startTime: '', endTime: '', description: '' }
    
    await loadUpcomingBookings(roomStore.selectedRoom.id)
  } catch (error) {
    // Error is handled by the store
  }
}

const formatDateTime = (dateString: string) => {
  const date = new Date(dateString)
  return date.toLocaleString('en-US', {
    month: 'short',
    day: 'numeric',
    hour: '2-digit',
    minute: '2-digit'
  })
}

const formatTime = (dateString: string) => {
  const date = new Date(dateString)
  return date.toLocaleTimeString('en-US', {
    hour: '2-digit',
    minute: '2-digit'
  })
}

const goBack = () => {
  router.push('/rooms')
}
</script>

<style scoped>
.room-detail-container {
  padding: 2rem 0;
}

.back-button {
  background: none;
  border: none;
  color: #3498db;
  font-size: 1rem;
  cursor: pointer;
  margin-bottom: 2rem;
  padding: 0.5rem;
  transition: color 0.3s;
}

.back-button:hover {
  color: #2980b9;
}

.content-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 2rem;
}

@media (max-width: 768px) {
  .content-grid {
    grid-template-columns: 1fr;
  }
}

.room-info h1 {
  margin-bottom: 1.5rem;
  color: #2c3e50;
}

.info-item {
  margin-bottom: 1rem;
  padding-bottom: 1rem;
  border-bottom: 1px solid #eee;
}

.info-item:last-child {
  border-bottom: none;
}

.status-badge {
  padding: 0.25rem 0.75rem;
  border-radius: 12px;
  font-size: 0.875rem;
  font-weight: 600;
  margin-left: 0.5rem;
}

.status-badge.available {
  background-color: #d4edda;
  color: #155724;
}

.status-badge.unavailable {
  background-color: #f8d7da;
  color: #721c24;
}

.booking-section {
  display: flex;
  flex-direction: column;
  gap: 2rem;
}

h2, h3 {
  margin-bottom: 1.5rem;
  color: #2c3e50;
}

.form-group {
  margin-bottom: 1.5rem;
}

label {
  display: block;
  margin-bottom: 0.5rem;
  color: #555;
  font-weight: 500;
}

input, textarea {
  width: 100%;
  padding: 0.75rem;
  border: 1px solid #ddd;
  border-radius: 4px;
  font-size: 1rem;
}

input:focus, textarea:focus {
  outline: none;
  border-color: #3498db;
}

.btn-primary {
  width: 100%;
  padding: 0.75rem;
  background-color: #3498db;
  color: white;
  border: none;
  border-radius: 4px;
  font-size: 1rem;
  cursor: pointer;
  transition: background-color 0.3s;
}

.btn-primary:hover:not(:disabled) {
  background-color: #2980b9;
}

.btn-primary:disabled {
  background-color: #95a5a6;
  cursor: not-allowed;
}

.upcoming-bookings {
  margin-top: 0;
}

.no-bookings {
  text-align: center;
  padding: 2rem;
  color: #666;
}

.bookings-list {
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

.booking-item {
  padding: 1rem;
  background-color: #f8f9fa;
  border-radius: 4px;
  border-left: 3px solid #3498db;
}

.booking-title {
  font-weight: 600;
  color: #2c3e50;
  margin-bottom: 0.5rem;
}

.booking-time {
  color: #666;
  font-size: 0.9rem;
}

.booking-user {
  color: #999;
  font-size: 0.85rem;
  margin-top: 0.25rem;
}
</style>
