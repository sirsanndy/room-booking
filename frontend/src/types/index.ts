export interface User {
  id: number
  username: string
  email: string
  fullName?: string
  roles: string[]
}

export interface LoginRequest {
  username: string
  password: string
}

export interface SignupRequest {
  username: string
  password: string
  email: string
  fullName: string
}

export interface AuthResponse {
  token: string
  type: string
  id: number
  username: string
  email: string
  roles: string[]
  expiresAt: number // Timestamp in milliseconds when token expires
}

export interface MeetingRoom {
  id: number
  name: string
  capacity: number
  description: string
  location: string
  available: boolean
  facilities: string
  createdAt: string
}

export interface Booking {
  id: number
  roomId: number
  roomName: string
  userId: number
  username: string
  startTime: string
  endTime: string
  title: string
  description: string
  status: string
  createdAt: string
}

export interface BookingRequest {
  roomId: number
  startTime: string
  endTime: string
  title: string
  description?: string
}

export interface MessageResponse {
  message: string
}

export interface CalendarEvent {
  id: number
  title: string
  start: string
  end: string
  description?: string
  roomName?: string
  roomId?: number
  status?: string
  type: 'booking' | 'holiday'
  color: string
}

export interface DashboardStats {
  totalBookings: number
  upcomingBookings: number
  completedBookings: number
  mostBookedRooms: string[]
}

export interface Dashboard {
  events: CalendarEvent[]
  stats: DashboardStats
}
