-- Update Demo Users with SHA-256 Hashed Passwords
-- After implementing frontend SHA-256 hashing, we need to update demo users
-- The password "password123" when hashed with SHA-256 becomes:
-- ef92b778bafe771e89245b89ecbc08a44a4e166c06659911881f383d4473e94f

-- Note: The backend will apply BCrypt on top of this SHA-256 hash
-- So the final stored password will be: BCrypt(SHA-256("password123"))

-- For new signups through the frontend, this happens automatically:
-- 1. User enters "password123"
-- 2. Frontend computes SHA-256: "ef92b778bafe771e..."
-- 3. Backend applies BCrypt on the SHA-256 hash
-- 4. Stored in DB: "$2a$10$..."

-- For existing users, you'll need to:
-- 1. Either have them reset password through the app (recommended)
-- 2. Or manually update with BCrypt(SHA-256(their_password))

-- To generate new BCrypt hashes for testing, use this Java snippet:
-- String sha256Hash = "ef92b778bafe771e89245b89ecbc08a44a4e166c06659911881f383d4473e94f";
-- String bcryptHash = passwordEncoder.encode(sha256Hash);
-- System.out.println(bcryptHash);

-- Example updates (using pre-generated BCrypt hashes of SHA-256("password123")):
-- UPDATE users SET password = '$2a$10$...' WHERE username = 'john';
-- UPDATE users SET password = '$2a$10$...' WHERE username = 'jane';

-- Important: After implementing this security update:
-- - Existing users will need to reset their passwords OR
-- - You need to regenerate their password hashes using the new method OR
-- - Use the signup page to create new test users

-- Security Notes:
-- 1. Never store passwords in this file in production
-- 2. This file is for development/testing only
-- 3. Always use environment variables for sensitive data
-- 4. Rotate database credentials regularly
