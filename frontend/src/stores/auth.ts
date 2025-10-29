import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import apiService from '@/services/api'
import type { User, LoginRequest, SignupRequest, AuthResponse } from '@/types'

export const useAuthStore = defineStore('auth', () => {
  const user = ref<User | null>(null)
  const token = ref<string | null>(null)
  const loading = ref(false)
  const error = ref<string | null>(null)

  const isAuthenticated = computed(() => !!token.value)

  // Initialize from localStorage
  const initializeAuth = () => {
    const storedToken = localStorage.getItem('token')
    const storedUser = localStorage.getItem('user')
    
    if (storedToken && storedUser) {
      token.value = storedToken
      user.value = JSON.parse(storedUser)
    }
  }

  const login = async (credentials: LoginRequest) => {
    try {
      loading.value = true
      error.value = null
      
      const response: AuthResponse = await apiService.login(credentials)
      
      token.value = response.token
      user.value = {
        id: response.id,
        username: response.username,
        email: response.email,
        roles: response.roles
      }
      
      localStorage.setItem('token', response.token)
      localStorage.setItem('user', JSON.stringify(user.value))
      
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
      
      await apiService.signup(userData)
      
      return true
    } catch (err: any) {
      error.value = err.response?.data?.message || 'Signup failed'
      return false
    } finally {
      loading.value = false
    }
  }

  const logout = () => {
    user.value = null
    token.value = null
    localStorage.removeItem('token')
    localStorage.removeItem('user')
  }

  // Initialize on store creation
  initializeAuth()

  return {
    user,
    token,
    loading,
    error,
    isAuthenticated,
    login,
    signup,
    logout
  }
})
