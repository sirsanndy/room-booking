<template>
  <div class="dashboard-container">
    <h1>Dashboard</h1>
    
    <div v-if="loading" class="loading">Loading dashboard...</div>
    <div v-else-if="error" class="error-message">{{ error }}</div>
    
    <div v-else class="dashboard-content" :class="{ 'with-sidebar': showEventSidebar }">
      <!-- Stats Cards -->
      <div class="stats-grid">
        <div class="stat-card card">
          <div class="stat-icon">📊</div>
          <div class="stat-info">
            <div class="stat-value">{{ dashboard?.stats.totalBookings || 0 }}</div>
            <div class="stat-label">Total Bookings</div>
          </div>
        </div>
        
        <div class="stat-card card">
          <div class="stat-icon">📅</div>
          <div class="stat-info">
            <div class="stat-value">{{ dashboard?.stats.upcomingBookings || 0 }}</div>
            <div class="stat-label">Upcoming</div>
          </div>
        </div>
        
        <div class="stat-card card">
          <div class="stat-icon">✅</div>
          <div class="stat-info">
            <div class="stat-value">{{ dashboard?.stats.completedBookings || 0 }}</div>
            <div class="stat-label">Completed</div>
          </div>
        </div>
        
        <div class="stat-card card">
          <div class="stat-icon">🏢</div>
          <div class="stat-info">
            <div class="stat-value">{{ dashboard?.stats.mostBookedRooms.length || 0 }}</div>
            <div class="stat-label">Favorite Rooms</div>
          </div>
        </div>
      </div>
      
      <!-- Most Booked Rooms -->
      <div v-if="dashboard?.stats.mostBookedRooms.length" class="card most-booked">
        <h3>🏆 Most Booked Rooms</h3>
        <div class="rooms-list">
          <div
            v-for="(room, index) in dashboard.stats.mostBookedRooms"
            :key="index"
            class="room-item"
          >
            <span class="room-rank">{{ index + 1 }}</span>
            <span class="room-name">{{ room }}</span>
          </div>
        </div>
      </div>
      
      <!-- Calendar -->
      <div class="card calendar-section">
        <h2>📅 My Calendar</h2>
        <div class="calendar-legend">
          <span class="legend-item">
            <span class="legend-color" style="background: #3498db"></span>
            My Bookings
          </span>
          <span class="legend-item">
            <span class="legend-color" style="background: #e74c3c"></span>
            Holidays (No booking allowed)
          </span>
          <span class="legend-item">
            <span class="legend-color" style="background: #95a5a6"></span>
            Weekends (No booking allowed)
          </span>
        </div>
        <p class="calendar-hint">💡 <strong>Tip:</strong> Click on any date/time to create a new booking, or click on an event to view details</p>
        <FullCalendar
          v-if="calendarOptions"
          :options="calendarOptions"
        />
      </div>
      
      <!-- Upcoming Events List -->
      <div class="card events-list">
        <h3>🗓️ Upcoming Events</h3>
        <div v-if="upcomingEvents.length === 0" class="no-events">
          No upcoming events
        </div>
        <div v-else class="events">
          <div
            v-for="event in upcomingEvents"
            :key="event.id"
            class="event-item"
            :class="{ 'holiday-event': event.type === 'holiday' }"
          >
            <div class="event-date">
              <div class="event-day">{{ formatDay(event.start) }}</div>
              <div class="event-month">{{ formatMonth(event.start) }}</div>
            </div>
            <div class="event-details">
              <div class="event-title">{{ event.title }}</div>
              <div class="event-meta">
                <span v-if="event.type === 'booking'" class="event-time">
                  {{ formatTime(event.start) }} - {{ formatTime(event.end) }}
                </span>
                <span v-if="event.roomName" class="event-room">📍 {{ event.roomName }}</span>
                <span v-if="event.type === 'holiday'" class="event-badge holiday-badge">🎉 Holiday</span>
              </div>
              <div v-if="event.description" class="event-description">
                {{ event.description }}
              </div>
            </div>
          </div>
        </div>
      </div>
      
      <!-- Booking Restrictions Info -->
      <div class="card info-card">
        <h3>ℹ️ Booking Rules</h3>
        <ul class="rules-list">
          <li>⏰ Bookings are allowed only between <strong>7:00 AM and 10:00 PM</strong></li>
          <li>📅 No bookings on <strong>weekends</strong> (Saturday & Sunday)</li>
          <li>🎉 No bookings on <strong>holidays</strong></li>
          <li>📝 Bookings cannot span multiple days</li>
          <li>✅ Rooms must be booked in advance</li>
          <li>⏱️ Maximum <strong>9 hours</strong> of bookings per day</li>
          <li>🚫 Cannot book multiple rooms at the same time</li>
        </ul>
      </div>
    </div>
    
    <!-- Booking Modal -->
    <div v-if="showBookingModal" class="modal-overlay" @click="closeBookingModal">
      <div class="modal-content" @click.stop>
        <div class="modal-header">
          <h2>📅 New Booking</h2>
          <button class="close-btn" @click="closeBookingModal">&times;</button>
        </div>
        
        <div class="modal-body">
          <div v-if="bookingError" class="error-message">{{ bookingError }}</div>
          
          <div class="form-group">
            <label>📍 Meeting Room *</label>
            <select v-model="bookingForm.roomId" required>
              <option value="">Select a room</option>
              <option v-for="room in availableRooms" :key="room.id" :value="room.id">
                {{ room.name }} (Capacity: {{ room.capacity }})
              </option>
            </select>
          </div>
          
          <div class="form-group">
            <label>📅 Date *</label>
            <input
              type="date"
              v-model="bookingForm.date"
              :min="todayDate"
              required
            />
          </div>
          
          <div class="form-row">
            <div class="form-group">
              <label>🕐 Start Time *</label>
              <input
                type="time"
                v-model="bookingForm.startTime"
                min="07:00"
                max="22:00"
                required
              />
            </div>
            
            <div class="form-group">
              <label>🕐 End Time *</label>
              <input
                type="time"
                v-model="bookingForm.endTime"
                min="07:00"
                max="22:00"
                required
              />
            </div>
          </div>
          
          <div class="form-group">
            <label>📝 Title *</label>
            <input
              type="text"
              v-model="bookingForm.title"
              placeholder="e.g., Team Meeting, Client Presentation"
              required
            />
          </div>
          
          <div class="form-group">
            <label>📄 Description (Optional)</label>
            <textarea
              v-model="bookingForm.description"
              placeholder="Add any additional details..."
              rows="3"
            ></textarea>
          </div>
        </div>
        
        <div class="modal-footer">
          <button class="btn-secondary" @click="closeBookingModal">Cancel</button>
          <button class="btn-primary" @click="handleCreateBooking" :disabled="isSubmitting">
            {{ isSubmitting ? 'Creating...' : '✓ Create Booking' }}
          </button>
        </div>
      </div>
    </div>
    
    <!-- Event Detail Sidebar -->
    <div v-if="showEventSidebar" class="sidebar-overlay" @click="closeEventSidebar">
      <div class="sidebar-content" @click.stop>
        <div class="sidebar-header">
          <h2>Event Details</h2>
          <button class="close-btn" @click="closeEventSidebar">&times;</button>
        </div>
        
        <div v-if="selectedEvent" class="sidebar-body">
          <div class="event-detail-card">
            <div class="detail-icon" :style="{ background: selectedEvent.color }">
              {{ selectedEvent.type === 'holiday' ? '🎉' : '📅' }}
            </div>
            <h3>{{ selectedEvent.title }}</h3>
            
            <div class="detail-section">
              <div class="detail-item">
                <span class="detail-label">📅 Date:</span>
                <span class="detail-value">{{ formatFullDate(selectedEvent.start) }}</span>
              </div>
              
              <div v-if="selectedEvent.type === 'booking'" class="detail-item">
                <span class="detail-label">🕐 Time:</span>
                <span class="detail-value">
                  {{ formatTime(selectedEvent.start) }} - {{ formatTime(selectedEvent.end) }}
                </span>
              </div>
              
              <div v-if="selectedEvent.type === 'booking'" class="detail-item">
                <span class="detail-label">⏱️ Duration:</span>
                <span class="detail-value">{{ calculateDuration(selectedEvent.start, selectedEvent.end) }}</span>
              </div>
              
              <div v-if="selectedEvent.roomName" class="detail-item">
                <span class="detail-label">📍 Room:</span>
                <span class="detail-value">{{ selectedEvent.roomName }}</span>
              </div>
              
              <div v-if="selectedEvent.status" class="detail-item">
                <span class="detail-label">✓ Status:</span>
                <span class="detail-value status-badge" :class="`status-${selectedEvent.status?.toLowerCase()}`">
                  {{ selectedEvent.status }}
                </span>
              </div>
              
              <div v-if="selectedEvent.description" class="detail-item full-width">
                <span class="detail-label">📄 Description:</span>
                <p class="detail-description">{{ selectedEvent.description }}</p>
              </div>
            </div>
            
            <div v-if="selectedEvent.type === 'booking'" class="sidebar-actions">
              <button class="btn-primary" @click="goToRoomDetail">
                View Room Details
              </button>
              <button class="btn-danger" @click="handleCancelBooking" :disabled="isCancelling">
                {{ isCancelling ? 'Cancelling...' : 'Cancel Booking' }}
              </button>
            </div>
            
            <div v-else class="holiday-notice">
              <p>🎉 This is a holiday. No bookings are allowed on this date.</p>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import FullCalendar from '@fullcalendar/vue3'
import dayGridPlugin from '@fullcalendar/daygrid'
import timeGridPlugin from '@fullcalendar/timegrid'
import interactionPlugin from '@fullcalendar/interaction'
import type { CalendarOptions, EventClickArg } from '@fullcalendar/core'
import type { DateClickArg } from '@fullcalendar/interaction'
import apiService from '@/services/api'
import type { Dashboard, CalendarEvent, MeetingRoom, BookingRequest } from '@/types'
import { useRouter } from 'vue-router'

const router = useRouter()
const dashboard = ref<Dashboard | null>(null)
const loading = ref(false)
const error = ref<string | null>(null)

// Booking modal state
const showBookingModal = ref(false)
const bookingError = ref<string | null>(null)
const isSubmitting = ref(false)
const availableRooms = ref<MeetingRoom[]>([])
const todayDate = computed(() => {
  const today = new Date()
  return today.toISOString().split('T')[0]
})

const bookingForm = ref({
  roomId: '',
  date: '',
  startTime: '09:00',
  endTime: '10:00',
  title: '',
  description: ''
})

// Event sidebar state
const showEventSidebar = ref(false)
const selectedEvent = ref<CalendarEvent | null>(null)
const isCancelling = ref(false)

const upcomingEvents = computed(() => {
  if (!dashboard.value) return []
  const now = new Date()
  return dashboard.value.events
    .filter(event => new Date(event.start) >= now)
    .sort((a, b) => new Date(a.start).getTime() - new Date(b.start).getTime())
    .slice(0, 5)
})

const calendarOptions = computed<CalendarOptions>(() => ({
  plugins: [dayGridPlugin, timeGridPlugin, interactionPlugin],
  initialView: 'dayGridMonth',
  headerToolbar: {
    left: 'prev,next today',
    center: 'title',
    right: 'dayGridMonth,timeGridWeek,timeGridDay'
  },
  events: dashboard.value?.events.map(event => ({
    id: String(event.id),
    title: event.title,
    start: event.start,
    end: event.end,
    backgroundColor: event.color,
    borderColor: event.color,
    extendedProps: {
      type: event.type,
      roomName: event.roomName,
      roomId: event.roomId,
      description: event.description,
      status: event.status
    }
  })) || [],
  dateClick: handleDateClick,
  eventClick: handleEventClick,
  selectable: true,
  selectMirror: true,
  height: 'auto',
  slotMinTime: '07:00:00',
  slotMaxTime: '22:00:00',
  slotDuration: '01:00:00',
  snapDuration: '01:00:00',
  businessHours: {
    daysOfWeek: [1, 2, 3, 4, 5], // Monday - Friday
    startTime: '07:00',
    endTime: '22:00'
  },
  weekends: true,
  dayCellClassNames: (arg) => {
    const day = arg.date.getDay()
    if (day === 0 || day === 6) {
      return ['weekend-day']
    }
    return []
  },
  nowIndicator: true
}))

onMounted(async () => {
  await loadDashboard()
  await loadAvailableRooms()
})

const loadDashboard = async () => {
  loading.value = true
  error.value = null
  try {
    dashboard.value = await apiService.getDashboard()
  } catch (err: any) {
    error.value = err.response?.data?.message || 'Failed to load dashboard'
  } finally {
    loading.value = false
  }
}

const loadAvailableRooms = async () => {
  try {
    availableRooms.value = await apiService.getAvailableRooms()
  } catch (err: any) {
    console.error('Failed to load rooms:', err)
  }
}

// Calendar event handlers
const handleDateClick = (arg: DateClickArg) => {
  const clickedDate = new Date(arg.date)
  const today = new Date()
  today.setHours(0, 0, 0, 0)
  
  // Check if date is in the past
  if (clickedDate < today) {
    bookingError.value = 'Cannot book dates in the past'
    return
  }
  
  // Check if it's a weekend
  const day = clickedDate.getDay()
  if (day === 0 || day === 6) {
    bookingError.value = 'Cannot book on weekends (Saturday & Sunday)'
    return
  }
  
  // Check if it's a holiday
  const isHoliday = dashboard.value?.events.some(event => 
    event.type === 'holiday' && 
    new Date(event.start).toDateString() === clickedDate.toDateString()
  )
  
  if (isHoliday) {
    bookingError.value = 'Cannot book on holidays'
    return
  }
  
  // Open booking modal with pre-filled date
  bookingForm.value.date = arg.dateStr
  
  // If in time grid view, pre-fill time
  if (arg.view.type.includes('timeGrid')) {
    const hours = clickedDate.getHours().toString().padStart(2, '0')
    bookingForm.value.startTime = `${hours}:00`
    const endHours = (clickedDate.getHours() + 1).toString().padStart(2, '0')
    bookingForm.value.endTime = `${endHours}:00`
  }
  
  bookingError.value = null
  showBookingModal.value = true
}

const handleEventClick = (arg: EventClickArg) => {
  const event = dashboard.value?.events.find(e => String(e.id) === arg.event.id)
  if (event) {
    selectedEvent.value = event
    showEventSidebar.value = true
  }
}

// Booking modal functions
const closeBookingModal = () => {
  showBookingModal.value = false
  bookingError.value = null
  bookingForm.value = {
    roomId: '',
    date: '',
    startTime: '09:00',
    endTime: '10:00',
    title: '',
    description: ''
  }
}

const handleCreateBooking = async () => {
  bookingError.value = null
  
  // Validation
  if (!bookingForm.value.roomId || !bookingForm.value.date || 
      !bookingForm.value.startTime || !bookingForm.value.endTime || 
      !bookingForm.value.title) {
    bookingError.value = 'Please fill in all required fields'
    return
  }
  
  // Validate time range
  if (bookingForm.value.startTime >= bookingForm.value.endTime) {
    bookingError.value = 'End time must be after start time'
    return
  }
  
  // Validate business hours (7 AM - 10 PM)
  const startHour = parseInt(bookingForm.value.startTime.split(':')[0])
  const endHour = parseInt(bookingForm.value.endTime.split(':')[0])
  
  if (startHour < 7 || startHour >= 22) {
    bookingError.value = 'Start time must be between 7:00 AM and 10:00 PM'
    return
  }
  
  if (endHour > 22 || (endHour === 22 && bookingForm.value.endTime !== '22:00')) {
    bookingError.value = 'End time must be before or at 10:00 PM'
    return
  }
  
  isSubmitting.value = true
  
  try {
    const bookingRequest: BookingRequest = {
      roomId: Number(bookingForm.value.roomId),
      startTime: `${bookingForm.value.date}T${bookingForm.value.startTime}:00`,
      endTime: `${bookingForm.value.date}T${bookingForm.value.endTime}:00`,
      title: bookingForm.value.title,
      description: bookingForm.value.description || undefined
    }
    
    await apiService.createBooking(bookingRequest)
    
    // Reload dashboard to show new booking
    await loadDashboard()
    
    // Close modal
    closeBookingModal()
    
    // Show success message (you can add a toast notification here)
    alert('Booking created successfully!')
  } catch (err: any) {
    bookingError.value = err.response?.data?.message || 'Failed to create booking'
  } finally {
    isSubmitting.value = false
  }
}

// Event sidebar functions
const closeEventSidebar = () => {
  showEventSidebar.value = false
  selectedEvent.value = null
}

const goToRoomDetail = () => {
  if (selectedEvent.value?.roomId) {
    router.push(`/rooms/${selectedEvent.value.roomId}`)
  }
}

const handleCancelBooking = async () => {
  if (!selectedEvent.value || selectedEvent.value.type !== 'booking') return
  
  if (!confirm('Are you sure you want to cancel this booking?')) {
    return
  }
  
  isCancelling.value = true
  
  try {
    await apiService.cancelBooking(selectedEvent.value.id)
    
    // Reload dashboard
    await loadDashboard()
    
    // Close sidebar
    closeEventSidebar()
    
    alert('Booking cancelled successfully!')
  } catch (err: any) {
    alert(err.response?.data?.message || 'Failed to cancel booking')
  } finally {
    isCancelling.value = false
  }
}

// Formatting functions

const formatDay = (dateString: string) => {
  const date = new Date(dateString)
  return date.getDate()
}

const formatMonth = (dateString: string) => {
  const date = new Date(dateString)
  return date.toLocaleDateString('en-US', { month: 'short' })
}

const formatTime = (dateString: string) => {
  const date = new Date(dateString)
  return date.toLocaleTimeString('en-US', { hour: '2-digit', minute: '2-digit' })
}

const formatFullDate = (dateString: string) => {
  const date = new Date(dateString)
  return date.toLocaleDateString('en-US', { 
    weekday: 'long', 
    year: 'numeric', 
    month: 'long', 
    day: 'numeric' 
  })
}

const calculateDuration = (startString: string, endString: string) => {
  const start = new Date(startString)
  const end = new Date(endString)
  const durationMs = end.getTime() - start.getTime()
  const hours = Math.floor(durationMs / (1000 * 60 * 60))
  const minutes = Math.floor((durationMs % (1000 * 60 * 60)) / (1000 * 60))
  
  if (minutes === 0) {
    return `${hours} hour${hours !== 1 ? 's' : ''}`
  }
  return `${hours}h ${minutes}m`
}
</script>

<style scoped>
.dashboard-container {
  padding: 2rem 0;
}

h1 {
  margin-bottom: 2rem;
  color: #2c3e50;
}

.dashboard-content {
  display: flex;
  flex-direction: column;
  gap: 2rem;
}

.stats-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  gap: 1.5rem;
}

.stat-card {
  display: flex;
  align-items: center;
  gap: 1.5rem;
  padding: 1.5rem;
  transition: transform 0.3s;
}

.stat-card:hover {
  transform: translateY(-4px);
}

.stat-icon {
  font-size: 3rem;
}

.stat-info {
  flex: 1;
}

.stat-value {
  font-size: 2rem;
  font-weight: bold;
  color: #2c3e50;
}

.stat-label {
  color: #666;
  font-size: 0.9rem;
  margin-top: 0.25rem;
}

.most-booked {
  margin-top: 0;
}

.most-booked h3 {
  margin-bottom: 1rem;
  color: #2c3e50;
}

.rooms-list {
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
}

.room-item {
  display: flex;
  align-items: center;
  gap: 1rem;
  padding: 0.75rem;
  background: #f8f9fa;
  border-radius: 6px;
}

.room-rank {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 32px;
  height: 32px;
  background: #3498db;
  color: white;
  border-radius: 50%;
  font-weight: bold;
}

.room-name {
  flex: 1;
  font-weight: 500;
  color: #2c3e50;
}

.calendar-section {
  margin-top: 0;
}

.calendar-section h2 {
  margin-bottom: 1rem;
  color: #2c3e50;
}

.calendar-legend {
  display: flex;
  flex-wrap: wrap;
  gap: 1.5rem;
  margin-bottom: 1.5rem;
  padding: 1rem;
  background: #f8f9fa;
  border-radius: 6px;
}

.legend-item {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  font-size: 0.9rem;
  color: #666;
}

.legend-color {
  width: 20px;
  height: 20px;
  border-radius: 4px;
}

:deep(.fc) {
  font-family: inherit;
}

:deep(.fc-daygrid-day.weekend-day) {
  background-color: #f8f9fa;
}

:deep(.fc-day-sat),
:deep(.fc-day-sun) {
  background-color: #f8f9fa;
}

:deep(.fc-event) {
  cursor: pointer;
}

.events-list h3 {
  margin-bottom: 1.5rem;
  color: #2c3e50;
}

.no-events {
  text-align: center;
  padding: 2rem;
  color: #666;
}

.events {
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

.event-item {
  display: flex;
  gap: 1.5rem;
  padding: 1.5rem;
  background: #f8f9fa;
  border-radius: 8px;
  border-left: 4px solid #3498db;
  transition: transform 0.2s;
}

.event-item:hover {
  transform: translateX(4px);
}

.event-item.holiday-event {
  border-left-color: #e74c3c;
  background: #fff5f5;
}

.event-date {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  min-width: 60px;
  padding: 0.75rem;
  background: white;
  border-radius: 8px;
  box-shadow: 0 2px 4px rgba(0,0,0,0.1);
}

.event-day {
  font-size: 1.75rem;
  font-weight: bold;
  color: #2c3e50;
  line-height: 1;
}

.event-month {
  font-size: 0.875rem;
  color: #666;
  text-transform: uppercase;
  margin-top: 0.25rem;
}

.event-details {
  flex: 1;
}

.event-title {
  font-weight: 600;
  color: #2c3e50;
  font-size: 1.1rem;
  margin-bottom: 0.5rem;
}

.event-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 1rem;
  margin-bottom: 0.5rem;
  font-size: 0.9rem;
  color: #666;
}

.event-time,
.event-room {
  display: flex;
  align-items: center;
  gap: 0.25rem;
}

.event-badge {
  padding: 0.25rem 0.75rem;
  border-radius: 12px;
  font-size: 0.85rem;
  font-weight: 600;
}

.holiday-badge {
  background: #ffe5e5;
  color: #c0392b;
}

.event-description {
  color: #666;
  font-size: 0.9rem;
  margin-top: 0.5rem;
}

.info-card h3 {
  margin-bottom: 1rem;
  color: #2c3e50;
}

.rules-list {
  list-style: none;
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
}

.rules-list li {
  padding: 0.75rem;
  background: #f8f9fa;
  border-radius: 6px;
  color: #555;
}

.calendar-hint {
  padding: 0.75rem 1rem;
  background: #e3f2fd;
  border-left: 4px solid #2196f3;
  border-radius: 6px;
  color: #1565c0;
  margin-bottom: 1.5rem;
  font-size: 0.95rem;
}

/* Modal Styles */
.modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.6);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
  padding: 1rem;
}

.modal-content {
  background: white;
  border-radius: 12px;
  max-width: 600px;
  width: 100%;
  max-height: 90vh;
  overflow-y: auto;
  box-shadow: 0 10px 40px rgba(0, 0, 0, 0.3);
}

.modal-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 1.5rem;
  border-bottom: 1px solid #e0e0e0;
}

.modal-header h2 {
  margin: 0;
  color: #2c3e50;
  font-size: 1.5rem;
}

.close-btn {
  background: none;
  border: none;
  font-size: 2rem;
  color: #999;
  cursor: pointer;
  padding: 0;
  width: 36px;
  height: 36px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
  transition: all 0.2s;
}

.close-btn:hover {
  background: #f0f0f0;
  color: #333;
}

.modal-body {
  padding: 1.5rem;
}

.form-group {
  margin-bottom: 1.25rem;
}

.form-group label {
  display: block;
  margin-bottom: 0.5rem;
  font-weight: 500;
  color: #2c3e50;
  font-size: 0.95rem;
}

.form-group input,
.form-group select,
.form-group textarea {
  width: 100%;
  padding: 0.75rem;
  border: 1px solid #ddd;
  border-radius: 6px;
  font-size: 1rem;
  transition: border-color 0.3s;
}

.form-group input:focus,
.form-group select:focus,
.form-group textarea:focus {
  outline: none;
  border-color: #3498db;
}

.form-row {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 1rem;
}

.modal-footer {
  padding: 1.5rem;
  border-top: 1px solid #e0e0e0;
  display: flex;
  gap: 1rem;
  justify-content: flex-end;
}

.btn-primary,
.btn-secondary,
.btn-danger {
  padding: 0.75rem 1.5rem;
  border: none;
  border-radius: 6px;
  font-size: 1rem;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.3s;
}

.btn-primary {
  background: #3498db;
  color: white;
}

.btn-primary:hover:not(:disabled) {
  background: #2980b9;
}

.btn-primary:disabled {
  background: #bdc3c7;
  cursor: not-allowed;
}

.btn-secondary {
  background: #ecf0f1;
  color: #2c3e50;
}

.btn-secondary:hover {
  background: #d5dbdb;
}

.btn-danger {
  background: #e74c3c;
  color: white;
}

.btn-danger:hover:not(:disabled) {
  background: #c0392b;
}

.btn-danger:disabled {
  background: #bdc3c7;
  cursor: not-allowed;
}

/* Sidebar Styles */
.sidebar-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.4);
  z-index: 999;
}

.sidebar-content {
  position: fixed;
  top: 0;
  right: 0;
  width: 450px;
  max-width: 90vw;
  height: 100vh;
  background: white;
  box-shadow: -4px 0 20px rgba(0, 0, 0, 0.2);
  overflow-y: auto;
  animation: slideInRight 0.3s ease-out;
}

@keyframes slideInRight {
  from {
    transform: translateX(100%);
  }
  to {
    transform: translateX(0);
  }
}

.sidebar-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 1.5rem;
  border-bottom: 1px solid #e0e0e0;
  background: #f8f9fa;
}

.sidebar-header h2 {
  margin: 0;
  color: #2c3e50;
  font-size: 1.5rem;
}

.sidebar-body {
  padding: 1.5rem;
}

.event-detail-card {
  background: white;
}

.detail-icon {
  width: 60px;
  height: 60px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 2rem;
  margin-bottom: 1rem;
}

.event-detail-card h3 {
  color: #2c3e50;
  margin-bottom: 1.5rem;
  font-size: 1.5rem;
}

.detail-section {
  display: flex;
  flex-direction: column;
  gap: 1rem;
  margin-bottom: 1.5rem;
}

.detail-item {
  display: flex;
  flex-direction: column;
  gap: 0.25rem;
}

.detail-item.full-width {
  grid-column: 1 / -1;
}

.detail-label {
  font-size: 0.85rem;
  color: #666;
  font-weight: 500;
}

.detail-value {
  font-size: 1rem;
  color: #2c3e50;
  font-weight: 500;
}

.detail-description {
  margin: 0.5rem 0 0 0;
  color: #555;
  line-height: 1.6;
}

.status-badge {
  display: inline-block;
  padding: 0.25rem 0.75rem;
  border-radius: 12px;
  font-size: 0.85rem;
  font-weight: 600;
}

.status-confirmed {
  background: #d4edda;
  color: #155724;
}

.status-cancelled {
  background: #f8d7da;
  color: #721c24;
}

.status-pending {
  background: #fff3cd;
  color: #856404;
}

.sidebar-actions {
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
  margin-top: 1.5rem;
  padding-top: 1.5rem;
  border-top: 1px solid #e0e0e0;
}

.sidebar-actions button {
  width: 100%;
}

.holiday-notice {
  padding: 1.5rem;
  background: #fff5f5;
  border: 1px solid #fee;
  border-radius: 8px;
  text-align: center;
  color: #c0392b;
  margin-top: 1rem;
}

.holiday-notice p {
  margin: 0;
  font-weight: 500;
}

.dashboard-content.with-sidebar {
  margin-right: 450px;
  transition: margin-right 0.3s ease-out;
}

@media (max-width: 768px) {
  .stats-grid {
    grid-template-columns: repeat(2, 1fr);
  }
  
  .event-item {
    flex-direction: column;
    gap: 1rem;
  }
  
  .event-date {
    align-self: flex-start;
  }
  
  .sidebar-content {
    width: 100%;
    max-width: 100vw;
  }
  
  .dashboard-content.with-sidebar {
    margin-right: 0;
  }
  
  .form-row {
    grid-template-columns: 1fr;
  }
  
  .modal-content {
    margin: 1rem;
  }
}
</style>
