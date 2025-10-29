import { defineStore } from 'pinia'
import { ref } from 'vue'
import apiService from '@/services/api'
import type { Booking, BookingRequest } from '@/types'

export const useBookingStore = defineStore('booking', () => {
  const bookings = ref<Booking[]>([])
  const myBookings = ref<Booking[]>([])
  const loading = ref(false)
  const error = ref<string | null>(null)

  const createBooking = async (bookingData: BookingRequest) => {
    try {
      loading.value = true
      error.value = null
      const booking = await apiService.createBooking(bookingData)
      myBookings.value.unshift(booking)
      return booking
    } catch (err: any) {
      error.value = err.response?.data?.message || 'Failed to create booking'
      throw err
    } finally {
      loading.value = false
    }
  }

  const fetchMyBookings = async () => {
    try {
      loading.value = true
      error.value = null
      myBookings.value = await apiService.getMyBookings()
    } catch (err: any) {
      error.value = err.response?.data?.message || 'Failed to fetch your bookings'
    } finally {
      loading.value = false
    }
  }

  const fetchBookingsByRoom = async (roomId: number) => {
    try {
      loading.value = true
      error.value = null
      bookings.value = await apiService.getBookingsByRoom(roomId)
      return bookings.value
    } catch (err: any) {
      error.value = err.response?.data?.message || 'Failed to fetch room bookings'
      return []
    } finally {
      loading.value = false
    }
  }

  const cancelBooking = async (bookingId: number) => {
    try {
      loading.value = true
      error.value = null
      await apiService.cancelBooking(bookingId)
      myBookings.value = myBookings.value.filter(b => b.id !== bookingId)
    } catch (err: any) {
      error.value = err.response?.data?.message || 'Failed to cancel booking'
      throw err
    } finally {
      loading.value = false
    }
  }

  return {
    bookings,
    myBookings,
    loading,
    error,
    createBooking,
    fetchMyBookings,
    fetchBookingsByRoom,
    cancelBooking
  }
})
