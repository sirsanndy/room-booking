package security

import (
	"crypto/sha256"
	"encoding/hex"
	"fmt"

	"golang.org/x/crypto/bcrypt"
)

const (
	// BCrypt cost factor (equivalent to Spring Security strength=12)
	BCryptCost = 12
)

// HashPassword hashes a password using BCrypt
// The input password is expected to be SHA-256 hashed by the frontend
// This creates a double-layer: BCrypt(SHA256(password))
func HashPassword(password string) (string, error) {
	// Convert to bytes
	bytes := []byte(password)

	// Generate BCrypt hash with cost 12 (includes automatic salting)
	hash, err := bcrypt.GenerateFromPassword(bytes, BCryptCost)
	if err != nil {
		return "", fmt.Errorf("failed to hash password: %w", err)
	}

	return string(hash), nil
}

// VerifyPassword verifies a password against a BCrypt hash
func VerifyPassword(password, hash string) bool {
	err := bcrypt.CompareHashAndPassword([]byte(hash), []byte(password))
	return err == nil
}

// SHA256Hash creates a SHA-256 hash of the input string
// Used for additional frontend hashing (client-side)
func SHA256Hash(input string) string {
	hasher := sha256.New()
	hasher.Write([]byte(input))
	return hex.EncodeToString(hasher.Sum(nil))
}

// ValidatePasswordStrength checks if password meets minimum requirements
// Returns true if password is strong enough
func ValidatePasswordStrength(password string) (bool, string) {
	if len(password) < 8 {
		return false, "Password must be at least 8 characters long"
	}

	// Password is already SHA-256 hashed from frontend
	// So we just check if it's the expected length (64 hex characters)
	if len(password) == 64 {
		// Hex-encoded SHA-256
		return true, ""
	}

	// If not hashed, validate plain password
	hasUpper := false
	hasLower := false
	hasDigit := false
	hasSpecial := false

	for _, char := range password {
		switch {
		case char >= 'A' && char <= 'Z':
			hasUpper = true
		case char >= 'a' && char <= 'z':
			hasLower = true
		case char >= '0' && char <= '9':
			hasDigit = true
		case char >= '!' && char <= '/' || char >= ':' && char <= '@' || char >= '[' && char <= '`' || char >= '{' && char <= '~':
			hasSpecial = true
		}
	}

	if !hasUpper {
		return false, "Password must contain at least one uppercase letter"
	}
	if !hasLower {
		return false, "Password must contain at least one lowercase letter"
	}
	if !hasDigit {
		return false, "Password must contain at least one digit"
	}
	if !hasSpecial {
		return false, "Password must contain at least one special character"
	}

	return true, ""
}
