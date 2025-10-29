import { defineStore } from 'pinia'
import { ref } from 'vue'
import apiService from '@/services/api'
import type { MeetingRoom } from '@/types'

export const useRoomStore = defineStore('room', () => {
  const rooms = ref<MeetingRoom[]>([])
  const selectedRoom = ref<MeetingRoom | null>(null)
  const loading = ref(false)
  const error = ref<string | null>(null)

  const fetchAllRooms = async () => {
    try {
      loading.value = true
      error.value = null
      rooms.value = await apiService.getAllRooms()
    } catch (err: any) {
      error.value = err.response?.data?.message || 'Failed to fetch rooms'
    } finally {
      loading.value = false
    }
  }

  const fetchAvailableRooms = async () => {
    try {
      loading.value = true
      error.value = null
      rooms.value = await apiService.getAvailableRooms()
    } catch (err: any) {
      error.value = err.response?.data?.message || 'Failed to fetch available rooms'
    } finally {
      loading.value = false
    }
  }

  const fetchRoomById = async (id: number) => {
    try {
      loading.value = true
      error.value = null
      selectedRoom.value = await apiService.getRoomById(id)
    } catch (err: any) {
      error.value = err.response?.data?.message || 'Failed to fetch room'
    } finally {
      loading.value = false
    }
  }

  return {
    rooms,
    selectedRoom,
    loading,
    error,
    fetchAllRooms,
    fetchAvailableRooms,
    fetchRoomById
  }
})
