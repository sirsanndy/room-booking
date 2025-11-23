<template>
  <Transition name="slide-down">
    <div v-if="showNotice" class="session-notice" :class="noticeClass">
      <div class="notice-content">
        <svg v-if="isExpiringSoon" class="icon warning" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke="currentColor">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z" />
        </svg>
        <svg v-else class="icon info" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke="currentColor">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M13 16h-1v-4h-1m1-4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
        </svg>
        <div class="notice-text">
          <strong v-if="isExpiringSoon">Session Expiring Soon!</strong>
          <strong v-else>Session Information</strong>
          <p>{{ message }}</p>
        </div>
        <button @click="dismissNotice" class="close-btn" aria-label="Close">
          <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke="currentColor">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12" />
          </svg>
        </button>
      </div>
    </div>
  </Transition>
</template>

<script setup lang="ts">
import { ref, computed, watch, onMounted, onUnmounted } from 'vue'
import { useAuthStore } from '@/stores/auth'
import { useRouter } from 'vue-router'

const authStore = useAuthStore()
const router = useRouter()

const showNotice = ref(false)
const dismissed = ref(false)
const updateInterval = ref<number | null>(null)

const isExpiringSoon = computed(() => authStore.isSessionExpiringSoon)

const message = computed(() => {
  const timeRemaining = authStore.getTimeUntilExpiration
  
  if (timeRemaining <= 0) {
    return 'Your session has expired. Please log in again.'
  }
  
  const minutes = Math.floor(timeRemaining / 60000)
  const seconds = Math.floor((timeRemaining % 60000) / 1000)
  
  if (minutes > 0) {
    return `Your session will expire in ${minutes} minute${minutes !== 1 ? 's' : ''}.`
  } else if (seconds > 0) {
    return `Your session will expire in ${seconds} second${seconds !== 1 ? 's' : ''}.`
  }
  
  return 'Your session is about to expire.'
})

const noticeClass = computed(() => {
  if (!authStore.isAuthenticated) {
    return 'expired'
  }
  return isExpiringSoon.value ? 'warning' : 'info'
})

const dismissNotice = () => {
  dismissed.value = true
  showNotice.value = false
}

// Watch for session expiration
watch(() => authStore.isSessionExpiringSoon, (expiringSoon) => {
  if (expiringSoon && !dismissed.value) {
    showNotice.value = true
  }
})

// Watch for session expired
watch(() => authStore.isAuthenticated, (authenticated) => {
  if (!authenticated && authStore.token === null) {
    // Session expired, show notice briefly then redirect
    showNotice.value = true
    dismissed.value = false
    
    setTimeout(() => {
      router.push('/login')
    }, 3000)
  }
})

// Update display every second when notice is shown
onMounted(() => {
  updateInterval.value = window.setInterval(() => {
    if (showNotice.value && authStore.isSessionExpiringSoon) {
      // Force reactivity update
      showNotice.value = true
    }
  }, 1000)
})

onUnmounted(() => {
  if (updateInterval.value) {
    clearInterval(updateInterval.value)
  }
})
</script>

<style scoped>
.session-notice {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  z-index: 9999;
  padding: 1rem;
  box-shadow: 0 4px 6px rgba(0, 0, 0, 0.1);
}

.session-notice.warning {
  background-color: #fef3c7;
  border-bottom: 3px solid #f59e0b;
  color: #92400e;
}

.session-notice.info {
  background-color: #dbeafe;
  border-bottom: 3px solid #3b82f6;
  color: #1e40af;
}

.session-notice.expired {
  background-color: #fee2e2;
  border-bottom: 3px solid #ef4444;
  color: #991b1b;
}

.notice-content {
  max-width: 1200px;
  margin: 0 auto;
  display: flex;
  align-items: center;
  gap: 1rem;
}

.icon {
  width: 1.5rem;
  height: 1.5rem;
  flex-shrink: 0;
}

.icon.warning {
  color: #f59e0b;
}

.icon.info {
  color: #3b82f6;
}

.notice-text {
  flex: 1;
}

.notice-text strong {
  display: block;
  font-weight: 600;
  margin-bottom: 0.25rem;
}

.notice-text p {
  margin: 0;
  font-size: 0.875rem;
}

.close-btn {
  background: none;
  border: none;
  cursor: pointer;
  padding: 0.25rem;
  color: currentColor;
  opacity: 0.7;
  transition: opacity 0.2s;
}

.close-btn:hover {
  opacity: 1;
}

.close-btn svg {
  width: 1.25rem;
  height: 1.25rem;
}

/* Transition animations */
.slide-down-enter-active,
.slide-down-leave-active {
  transition: all 0.3s ease;
}

.slide-down-enter-from {
  transform: translateY(-100%);
  opacity: 0;
}

.slide-down-leave-to {
  transform: translateY(-100%);
  opacity: 0;
}
</style>
