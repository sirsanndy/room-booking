import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import apiService from '@/services/api'
import type { User, LoginRequest, SignupRequest, AuthResponse } from '@/types'
import { hashPassword } from '@/utils/security'

export const useAuthStore = defineStore('auth', () => {
  const user = ref<User | null>(null)
  const token = ref<string | null>(null)
  const tokenExpiresAt = ref<number | null>(null)
  const loading = ref(false)
  const error = ref<string | null>(null)
  const sessionCheckInterval = ref<number | null>(null)

  const isAuthenticated = computed(() => {
    if (!token.value || !tokenExpiresAt.value) return false
    // Check if token is expired
    return Date.now() < tokenExpiresAt.value
  })

  // Check if session is expired
  const checkSessionExpiration = () => {
    if (tokenExpiresAt.value && Date.now() >= tokenExpiresAt.value) {
      console.log('Session expired, logging out...')
      logout()
      return true
    }
    return false
  }

  // Start session monitoring
  const startSessionMonitoring = () => {
    // Clear any existing interval
    if (sessionCheckInterval.value) {
      clearInterval(sessionCheckInterval.value)
    }
    
    // Check every 60 seconds (1 minute)
    sessionCheckInterval.value = window.setInterval(() => {
      checkSessionExpiration()
    }, 60000)
  }

  // Stop session monitoring
  const stopSessionMonitoring = () => {
    if (sessionCheckInterval.value) {
      clearInterval(sessionCheckInterval.value)
      sessionCheckInterval.value = null
    }
  }

  // Initialize from localStorage
  const initializeAuth = () => {
    const storedToken = localStorage.getItem('token')
    const storedUser = localStorage.getItem('user')
    const storedExpiresAt = localStorage.getItem('tokenExpiresAt')
    
    if (storedToken && storedUser && storedExpiresAt) {
      const expiresAt = parseInt(storedExpiresAt, 10)
      
      // Check if token is already expired
      if (Date.now() >= expiresAt) {
        console.log('Stored token is expired, clearing session')
        logout()
        return
      }
      
      token.value = storedToken
      user.value = JSON.parse(storedUser)
      tokenExpiresAt.value = expiresAt
      
      // Start monitoring session
      startSessionMonitoring()
    }
  }

  const login = async (credentials: LoginRequest) => {
    try {
      loading.value = true
      error.value = null
      
      // Hash password before sending to server for added security
      const hashedCredentials = {
        username: credentials.username,
        password: hashPassword(credentials.password)
      }
      
      const response: AuthResponse = await apiService.login(hashedCredentials)
      
      token.value = response.token
      tokenExpiresAt.value = response.expiresAt
      user.value = {
        id: response.id,
        username: response.username,
        email: response.email,
        roles: response.roles
      }
      
      localStorage.setItem('token', response.token)
      localStorage.setItem('tokenExpiresAt', response.expiresAt.toString())
      localStorage.setItem('user', JSON.stringify(user.value))
      
      // Start session monitoring
      startSessionMonitoring()
      
      return true
    } catch (err: any) {
      error.value = err.response?.data?.message || 'Login failed'
      return false
    } finally {
      loading.value = false
    }
  }

  const signup = async (userData: SignupRequest) => {
    try {
      loading.value = true
      error.value = null
      
      // Hash password before sending to server for added security
      const hashedUserData = {
        ...userData,
        password: hashPassword(userData.password)
      }
      
      await apiService.signup(hashedUserData)
      
      return true
    } catch (err: any) {
      error.value = err.response?.data?.message || 'Signup failed'
      return false
    } finally {
      loading.value = false
    }
  }

  const logout = () => {
    stopSessionMonitoring()
    user.value = null
    token.value = null
    tokenExpiresAt.value = null
    localStorage.removeItem('token')
    localStorage.removeItem('tokenExpiresAt')
    localStorage.removeItem('user')
  }

  // Get time remaining until session expires (in milliseconds)
  const getTimeUntilExpiration = computed(() => {
    if (!tokenExpiresAt.value) return 0
    const remaining = tokenExpiresAt.value - Date.now()
    return remaining > 0 ? remaining : 0
  })

  // Check if session will expire soon (within 5 minutes)
  const isSessionExpiringSoon = computed(() => {
    const fiveMinutes = 5 * 60 * 1000
    return getTimeUntilExpiration.value > 0 && getTimeUntilExpiration.value < fiveMinutes
  })

  // Initialize on store creation
  initializeAuth()

  return {
    user,
    token,
    tokenExpiresAt,
    loading,
    error,
    isAuthenticated,
    getTimeUntilExpiration,
    isSessionExpiringSoon,
    login,
    signup,
    logout,
    checkSessionExpiration
  }
})
