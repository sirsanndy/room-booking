import axios, { AxiosInstance, AxiosError } from 'axios'
import type {
  LoginRequest,
  SignupRequest,
  AuthResponse,
  MeetingRoom,
  Booking,
  BookingRequest,
  MessageResponse,
  Dashboard
} from '@/types'

class ApiService {
  private api: AxiosInstance

  constructor() {
    this.api = axios.create({
      baseURL: 'http://localhost:8080/api',
      headers: {
        'Content-Type': 'application/json'
      }
    })

    // Request interceptor to add token
    this.api.interceptors.request.use(
      (config) => {
        const token = localStorage.getItem('token')
        if (token) {
          config.headers.Authorization = `Bearer ${token}`
        }
        return config
      },
      (error) => {
        return Promise.reject(error)
      }
    )

    // Response interceptor to handle errors
    this.api.interceptors.response.use(
      (response) => response,
      (error: AxiosError) => {
        if (error.response?.status === 401) {
          localStorage.removeItem('token')
          localStorage.removeItem('user')
          window.location.href = '/login'
        }
        return Promise.reject(error)
      }
    )
  }

  // Auth endpoints
  async login(credentials: LoginRequest): Promise<AuthResponse> {
    const response = await this.api.post<AuthResponse>('/auth/signin', credentials)
    return response.data
  }

  async signup(userData: SignupRequest): Promise<MessageResponse> {
    const response = await this.api.post<MessageResponse>('/auth/signup', userData)
    return response.data
  }

  // Room endpoints
  async getAllRooms(): Promise<MeetingRoom[]> {
    const response = await this.api.get<MeetingRoom[]>('/rooms')
    return response.data
  }

  async getAvailableRooms(): Promise<MeetingRoom[]> {
    const response = await this.api.get<MeetingRoom[]>('/rooms/available')
    return response.data
  }

  async getRoomById(id: number): Promise<MeetingRoom> {
    const response = await this.api.get<MeetingRoom>(`/rooms/${id}`)
    return response.data
  }

  // Booking endpoints
  async createBooking(bookingData: BookingRequest): Promise<Booking> {
    const response = await this.api.post<Booking>('/bookings', bookingData)
    return response.data
  }

  async getMyBookings(): Promise<Booking[]> {
    const response = await this.api.get<Booking[]>('/bookings/my-bookings')
    return response.data
  }

  async getBookingsByRoom(roomId: number): Promise<Booking[]> {
    const response = await this.api.get<Booking[]>(`/bookings/room/${roomId}`)
    return response.data
  }

  async getAllUpcomingBookings(): Promise<Booking[]> {
    const response = await this.api.get<Booking[]>('/bookings')
    return response.data
  }

  async cancelBooking(bookingId: number): Promise<MessageResponse> {
    const response = await this.api.delete<MessageResponse>(`/bookings/${bookingId}`)
    return response.data
  }

  async checkAvailability(
    roomId: number,
    startTime: string,
    endTime: string
  ): Promise<MessageResponse> {
    const response = await this.api.get<MessageResponse>('/bookings/check-availability', {
      params: { roomId, startTime, endTime }
    })
    return response.data
  }

  // Dashboard endpoints
  async getDashboard(): Promise<Dashboard> {
    const response = await this.api.get<Dashboard>('/dashboard')
    return response.data
  }
}

export default new ApiService()
