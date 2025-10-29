<template>
  <div class="rooms-container">
    <h1>Available Meeting Rooms</h1>
    
    <div v-if="roomStore.loading" class="loading">Loading rooms...</div>
    <div v-else-if="roomStore.error" class="error-message">{{ roomStore.error }}</div>
    
    <div v-else class="rooms-grid">
      <div
        v-for="room in roomStore.rooms"
        :key="room.id"
        class="room-card card"
        @click="goToRoomDetail(room.id)"
      >
        <div class="room-header">
          <h3>{{ room.name }}</h3>
          <span :class="['status-badge', room.available ? 'available' : 'unavailable']">
            {{ room.available ? 'Available' : 'Unavailable' }}
          </span>
        </div>
        <div class="room-details">
          <p><strong>Location:</strong> {{ room.location }}</p>
          <p><strong>Capacity:</strong> {{ room.capacity }} people</p>
          <p><strong>Facilities:</strong> {{ room.facilities }}</p>
          <p class="description">{{ room.description }}</p>
        </div>
        <button class="btn-primary">View & Book</button>
      </div>
    </div>
    
    <div v-if="!roomStore.loading && roomStore.rooms.length === 0" class="no-rooms">
      <p>No meeting rooms available at the moment.</p>
    </div>
  </div>
</template>

<script setup lang="ts">
import { onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useRoomStore } from '@/stores/room'

const router = useRouter()
const roomStore = useRoomStore()

onMounted(() => {
  roomStore.fetchAvailableRooms()
})

const goToRoomDetail = (roomId: number) => {
  router.push(`/rooms/${roomId}`)
}
</script>

<style scoped>
.rooms-container {
  padding: 2rem 0;
}

h1 {
  margin-bottom: 2rem;
  color: #2c3e50;
}

.rooms-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(350px, 1fr));
  gap: 2rem;
}

.room-card {
  cursor: pointer;
  transition: transform 0.3s, box-shadow 0.3s;
}

.room-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 4px 12px rgba(0,0,0,0.15);
}

.room-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 1rem;
}

.room-header h3 {
  margin: 0;
  color: #2c3e50;
}

.status-badge {
  padding: 0.25rem 0.75rem;
  border-radius: 12px;
  font-size: 0.875rem;
  font-weight: 600;
}

.status-badge.available {
  background-color: #d4edda;
  color: #155724;
}

.status-badge.unavailable {
  background-color: #f8d7da;
  color: #721c24;
}

.room-details {
  margin-bottom: 1.5rem;
}

.room-details p {
  margin: 0.5rem 0;
  color: #555;
}

.description {
  margin-top: 1rem;
  padding-top: 1rem;
  border-top: 1px solid #eee;
  color: #666;
  font-style: italic;
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

.btn-primary:hover {
  background-color: #2980b9;
}

.no-rooms {
  text-align: center;
  padding: 3rem;
  color: #666;
}
</style>
