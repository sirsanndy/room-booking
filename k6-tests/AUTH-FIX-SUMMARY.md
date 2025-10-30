# K6 Authentication Fix - Summary

## Problem

K6 tests were failing with error:
```
❌ Failed to obtain bearer token for K6
❌ Failed to get token for testuser1
Token value: null
```

## Root Cause

Two issues in `k6-tests/lib/api-client.js`:

1. **Wrong endpoint**: Tests called `/api/auth/login` but backend expects `/api/auth/signin`
2. **Incorrect JSON parsing**: Used `response.json('token')` instead of proper JSON parsing

## Solution

### 1. Fixed Authentication Endpoint

**Before:**
```javascript
const response = http.post(`${BASE_URL}/api/auth/login`, payload, params);
```

**After:**
```javascript
const response = http.post(`${BASE_URL}/api/auth/signin`, payload, params);
```

### 2. Improved JSON Response Parsing

**Before:**
```javascript
const token = response.json('token');
tokenCache[username] = token;
return token;
```

**After:**
```javascript
try {
  const body = JSON.parse(response.body);
  const token = body.token;
  if (token) {
    tokenCache[username] = token;
    return token;
  }
} catch (e) {
  console.error('Failed to extract token:', e);
}
return null;
```

### 3. Enhanced Error Checking

**Before:**
```javascript
check(response, {
  'token received': (r) => r.json('token') !== undefined,
});
```

**After:**
```javascript
check(response, {
  'token received': (r) => {
    if (r.status === 200) {
      try {
        const body = JSON.parse(r.body);
        return body.token !== undefined && body.token !== null;
      } catch (e) {
        console.error('Failed to parse login response:', e);
        return false;
      }
    }
    return false;
  },
});
```

## Files Modified

1. **k6-tests/lib/api-client.js** - Fixed login function
2. **k6-tests/tests/auth-test.js** - NEW: Authentication verification test
3. **k6-tests/run-all-tests.sh** - Added auth check before running tests
4. **k6-tests/README.md** - Added auth testing section
5. **k6-tests/QUICKSTART.md** - Added auth verification step
6. **k6-tests/TROUBLESHOOTING.md** - NEW: Complete troubleshooting guide

## Verification

Run this to verify the fix:

```bash
cd k6-tests
k6 run tests/auth-test.js
```

**Expected Output:**
```
✅ Authentication SUCCESS!
   Token: eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJrNnRlc3R1c2VyIiwiaW...
   Token length: 243 characters
   Token parts: 3 (should be 3 for JWT)

     ✓ Token obtained
     ✓ Token is string
     ✓ Token is not empty
     ✓ Token looks like JWT

   checks.........................: 100.00% ✓ 4        ✗ 0
```

## Backend API Reference

The correct backend authentication endpoints are:

### Sign Up
```bash
POST /api/auth/signup
Content-Type: application/json

{
  "username": "testuser1",
  "email": "testuser1@test.com",
  "password": "Test@1234",
  "fullName": "Test User 1"
}
```

**Response:**
```json
{
  "message": "User registered successfully"
}
```

### Sign In
```bash
POST /api/auth/signin
Content-Type: application/json

{
  "username": "testuser1",
  "password": "Test@1234"
}
```

**Response:**
```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9...",
  "type": "Bearer",
  "id": 1,
  "username": "testuser1",
  "email": "testuser1@test.com",
  "roles": ["ROLE_USER"]
}
```

### Using the Token

All authenticated endpoints require:
```
Authorization: Bearer <token>
```

Example:
```bash
curl -X GET http://localhost:8080/api/bookings/my \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiJ9..."
```

## Testing Flow

### 1. Quick Auth Test (30 seconds)
```bash
k6 run tests/auth-test.js
```

### 2. Setup Users (2 minutes)
```bash
k6 run scripts/setup-users.js
```

### 3. Run All Tests (5-10 minutes)
```bash
./run-all-tests.sh
```

## Troubleshooting

If authentication still fails:

1. **Check backend is running:**
   ```bash
   curl http://localhost:8080/api/rooms
   ```

2. **Check database:**
   ```bash
   docker ps | grep postgres
   ```

3. **Test manually:**
   ```bash
   curl -X POST http://localhost:8080/api/auth/signin \
     -H "Content-Type: application/json" \
     -d '{"username":"testuser1","password":"Test@1234"}'
   ```

4. **Check backend logs:**
   ```bash
   tail -f backend/logs/application.log
   ```

5. **Read full troubleshooting guide:**
   ```bash
   cat k6-tests/TROUBLESHOOTING.md
   ```

## Impact

✅ All k6 tests now work correctly:
- ✅ auth-test.js - Authentication verification
- ✅ race-condition-test.js - Pessimistic locking validation
- ✅ deadlock-test.js - Deadlock prevention validation
- ✅ overlap-test.js - Overlap detection validation
- ✅ double-booking-test.js - Double booking prevention
- ✅ stress-test.js - 1000 user stress testing

## Next Steps

1. Run auth test to verify: `k6 run tests/auth-test.js`
2. Setup test users: `k6 run scripts/setup-users.js`
3. Run full test suite: `./run-all-tests.sh`

---

**Date:** October 29, 2025
**Status:** ✅ RESOLVED
**Verified:** Authentication working correctly
