<template>
  <div class="auth-container">
    <div class="auth-card card">
      <h2>Sign Up</h2>
      <form @submit.prevent="handleSignup">
        <div class="form-group">
          <label for="fullName">Full Name</label>
          <input
            id="fullName"
            v-model="userData.fullName"
            type="text"
            required
            placeholder="Enter your full name"
          />
        </div>
        <div class="form-group">
          <label for="username">Username</label>
          <input
            id="username"
            v-model="userData.username"
            type="text"
            required
            minlength="3"
            placeholder="Choose a username"
          />
        </div>
        <div class="form-group">
          <label for="email">Email</label>
          <input
            id="email"
            v-model="userData.email"
            type="email"
            required
            placeholder="Enter your email"
          />
        </div>
        <div class="form-group">
          <label for="password">Password</label>
          <input
            id="password"
            v-model="userData.password"
            type="password"
            required
            minlength="8"
            placeholder="Min 8 chars with uppercase, number & special char"
            @input="checkPasswordStrength"
          />
          <div v-if="passwordFeedback" class="password-feedback">
            <div class="password-strength">
              <div class="strength-bar" :class="passwordFeedback.strength"></div>
            </div>
            <p :class="passwordFeedback.isValid ? 'success-text' : 'warning-text'">
              {{ passwordFeedback.message }}
            </p>
            <div class="password-tips">
              <p class="tips-title">Password must contain:</p>
              <ul>
                <li :class="{ valid: hasMinLength }">✓ At least 8 characters</li>
                <li :class="{ valid: hasUpperCase }">✓ One uppercase letter</li>
                <li :class="{ valid: hasLowerCase }">✓ One lowercase letter</li>
                <li :class="{ valid: hasNumber }">✓ One number</li>
                <li :class="{ valid: hasSpecialChar }">✓ One special character</li>
              </ul>
            </div>
          </div>
        </div>
        <button type="submit" class="btn-primary" :disabled="authStore.loading || !passwordFeedback?.isValid">
          {{ authStore.loading ? 'Creating account...' : 'Sign Up' }}
        </button>
        <p v-if="authStore.error" class="error-message">{{ authStore.error }}</p>
        <p v-if="successMessage" class="success-message">{{ successMessage }}</p>
      </form>
      <p class="auth-footer">
        Already have an account? <router-link to="/login">Login</router-link>
      </p>
      <div class="security-notice">
        <p>🔒 <strong>Security Notice:</strong></p>
        <ul>
          <li>Your password is encrypted before transmission</li>
          <li>Passwords are securely hashed in our database</li>
          <li>We never store passwords in plain text</li>
        </ul>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import type { SignupRequest } from '@/types'
import { validatePasswordStrength } from '@/utils/security'

const router = useRouter()
const authStore = useAuthStore()

const userData = ref<SignupRequest>({
  username: '',
  password: '',
  email: '',
  fullName: ''
})

const successMessage = ref('')
const passwordFeedback = ref<ReturnType<typeof validatePasswordStrength> | null>(null)

// Individual validation checks for UI feedback
const hasMinLength = computed(() => userData.value.password.length >= 8)
const hasUpperCase = computed(() => /[A-Z]/.test(userData.value.password))
const hasLowerCase = computed(() => /[a-z]/.test(userData.value.password))
const hasNumber = computed(() => /\d/.test(userData.value.password))
const hasSpecialChar = computed(() => /[!@#$%^&*(),.?":{}|<>]/.test(userData.value.password))

const checkPasswordStrength = () => {
  if (userData.value.password) {
    passwordFeedback.value = validatePasswordStrength(userData.value.password)
  } else {
    passwordFeedback.value = null
  }
}

const handleSignup = async () => {
  // Validate password strength before submitting
  const validation = validatePasswordStrength(userData.value.password)
  if (!validation.isValid) {
    authStore.error = validation.message
    return
  }
  
  successMessage.value = ''
  const success = await authStore.signup(userData.value)
  if (success) {
    successMessage.value = 'Account created successfully! Redirecting to login...'
    setTimeout(() => {
      router.push('/login')
    }, 2000)
  }
}
</script>

<style scoped>
.auth-container {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: calc(100vh - 4rem);
  padding: 2rem;
}

.auth-card {
  width: 100%;
  max-width: 400px;
}

h2 {
  margin-bottom: 1.5rem;
  text-align: center;
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

input {
  width: 100%;
  padding: 0.75rem;
  border: 1px solid #ddd;
  border-radius: 4px;
  font-size: 1rem;
}

input:focus {
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

.auth-footer {
  margin-top: 1.5rem;
  text-align: center;
  color: #666;
}

.auth-footer a {
  color: #3498db;
  text-decoration: none;
}

.auth-footer a:hover {
  text-decoration: underline;
}

.password-feedback {
  margin-top: 0.75rem;
}

.password-strength {
  height: 4px;
  background-color: #e0e0e0;
  border-radius: 2px;
  overflow: hidden;
  margin-bottom: 0.5rem;
}

.strength-bar {
  height: 100%;
  transition: all 0.3s ease;
}

.strength-bar.weak {
  width: 25%;
  background-color: #e74c3c;
}

.strength-bar.medium {
  width: 50%;
  background-color: #f39c12;
}

.strength-bar.strong {
  width: 75%;
  background-color: #3498db;
}

.strength-bar.very-strong {
  width: 100%;
  background-color: #27ae60;
}

.success-text {
  color: #27ae60;
  font-size: 0.85rem;
  margin: 0.25rem 0;
}

.warning-text {
  color: #e74c3c;
  font-size: 0.85rem;
  margin: 0.25rem 0;
}

.password-tips {
  margin-top: 0.75rem;
  padding: 0.75rem;
  background-color: #f8f9fa;
  border-radius: 4px;
  border-left: 3px solid #3498db;
}

.tips-title {
  font-size: 0.85rem;
  font-weight: 600;
  color: #2c3e50;
  margin: 0 0 0.5rem 0;
}

.password-tips ul {
  list-style: none;
  padding: 0;
  margin: 0;
}

.password-tips li {
  font-size: 0.85rem;
  color: #666;
  padding: 0.25rem 0;
  transition: color 0.3s ease;
}

.password-tips li.valid {
  color: #27ae60;
  font-weight: 500;
}

.security-notice {
  margin-top: 1.5rem;
  padding: 1rem;
  background-color: #e8f5e9;
  border-radius: 4px;
  border-left: 3px solid #27ae60;
}

.security-notice p {
  margin: 0 0 0.5rem 0;
  color: #2c3e50;
  font-size: 0.9rem;
}

.security-notice ul {
  margin: 0;
  padding-left: 1.5rem;
  color: #555;
  font-size: 0.85rem;
}

.security-notice li {
  margin: 0.25rem 0;
}
</style>
