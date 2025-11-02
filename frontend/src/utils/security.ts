import CryptoJS from 'crypto-js'

/**
 * Security utility for password handling and encryption
 * 
 * Security Layers:
 * 1. Client-side: SHA-256 hash before transmission (prevents plaintext over network)
 * 2. Server-side: BCrypt hash for storage (prevents rainbow table attacks)
 * 3. Transport: HTTPS (prevents man-in-the-middle attacks)
 */

/**
 * Hash password using SHA-256 before sending to server
 * This prevents the password from being transmitted in plaintext
 * 
 * Note: This is NOT the final hash stored in database.
 * Server will apply BCrypt on top of this for additional security.
 * 
 * @param password - Plain text password
 * @returns SHA-256 hashed password in hex format
 */
export const hashPassword = (password: string): string => {
  return CryptoJS.SHA256(password).toString(CryptoJS.enc.Hex)
}

/**
 * Validate password strength
 * Requirements:
 * - Minimum 8 characters
 * - At least one uppercase letter
 * - At least one lowercase letter
 * - At least one number
 * - At least one special character
 * 
 * @param password - Password to validate
 * @returns Object with validation result and message
 */
export const validatePasswordStrength = (password: string): {
  isValid: boolean
  message: string
  strength: 'weak' | 'medium' | 'strong' | 'very-strong'
} => {
  const minLength = 8
  const hasUpperCase = /[A-Z]/.test(password)
  const hasLowerCase = /[a-z]/.test(password)
  const hasNumbers = /\d/.test(password)
  const hasSpecialChar = /[!@#$%^&*(),.?":{}|<>]/.test(password)
  
  let strength: 'weak' | 'medium' | 'strong' | 'very-strong' = 'weak'
  let strengthScore = 0
  
  if (password.length >= minLength) strengthScore++
  if (hasUpperCase) strengthScore++
  if (hasLowerCase) strengthScore++
  if (hasNumbers) strengthScore++
  if (hasSpecialChar) strengthScore++
  if (password.length >= 12) strengthScore++
  
  if (strengthScore <= 2) strength = 'weak'
  else if (strengthScore === 3) strength = 'medium'
  else if (strengthScore === 4) strength = 'strong'
  else strength = 'very-strong'
  
  // Validation errors
  if (password.length < minLength) {
    return {
      isValid: false,
      message: `Password must be at least ${minLength} characters long`,
      strength
    }
  }
  
  if (!hasUpperCase) {
    return {
      isValid: false,
      message: 'Password must contain at least one uppercase letter',
      strength
    }
  }
  
  if (!hasLowerCase) {
    return {
      isValid: false,
      message: 'Password must contain at least one lowercase letter',
      strength
    }
  }
  
  if (!hasNumbers) {
    return {
      isValid: false,
      message: 'Password must contain at least one number',
      strength
    }
  }
  
  if (!hasSpecialChar) {
    return {
      isValid: false,
      message: 'Password must contain at least one special character (!@#$%^&*)',
      strength
    }
  }
  
  return {
    isValid: true,
    message: 'Password is strong',
    strength
  }
}

/**
 * Generate a secure random password
 * @param length - Length of password (default: 16)
 * @returns Secure random password
 */
export const generateSecurePassword = (length: number = 16): string => {
  const uppercase = 'ABCDEFGHIJKLMNOPQRSTUVWXYZ'
  const lowercase = 'abcdefghijklmnopqrstuvwxyz'
  const numbers = '0123456789'
  const special = '!@#$%^&*()_+-=[]{}|;:,.<>?'
  const allChars = uppercase + lowercase + numbers + special
  
  let password = ''
  
  // Ensure at least one of each type
  password += uppercase[Math.floor(Math.random() * uppercase.length)]
  password += lowercase[Math.floor(Math.random() * lowercase.length)]
  password += numbers[Math.floor(Math.random() * numbers.length)]
  password += special[Math.floor(Math.random() * special.length)]
  
  // Fill the rest randomly
  for (let i = password.length; i < length; i++) {
    password += allChars[Math.floor(Math.random() * allChars.length)]
  }
  
  // Shuffle the password
  return password.split('').sort(() => Math.random() - 0.5).join('')
}

/**
 * Sanitize user input to prevent XSS attacks
 * @param input - User input string
 * @returns Sanitized string
 */
export const sanitizeInput = (input: string): string => {
  const div = document.createElement('div')
  div.textContent = input
  return div.innerHTML
}

/**
 * Check if browser supports crypto API
 * @returns Boolean indicating crypto support
 */
export const isCryptoSupported = (): boolean => {
  return !!(window.crypto && window.crypto.subtle)
}

/**
 * Get security recommendations based on environment
 * @returns Array of security recommendations
 */
export const getSecurityRecommendations = (): string[] => {
  const recommendations: string[] = []
  
  // Check if using HTTPS
  if (window.location.protocol !== 'https:' && window.location.hostname !== 'localhost') {
    recommendations.push('⚠️ Use HTTPS in production to encrypt data in transit')
  }
  
  // Check if crypto API is available
  if (!isCryptoSupported()) {
    recommendations.push('⚠️ Browser does not support modern crypto API')
  }
  
  // Check if local storage is available
  try {
    localStorage.setItem('test', 'test')
    localStorage.removeItem('test')
  } catch (e) {
    recommendations.push('⚠️ Local storage is disabled or not available')
  }
  
  return recommendations
}
