# K6 Tests Troubleshooting Guide

## Common Issues and Solutions

### 1. ❌ Authentication Failed - "Failed to obtain bearer token"

**Symptoms:**
```
❌ Failed to get token for testuser1
Token value: null
```

**Root Cause:**
The k6 tests were calling the wrong authentication endpoint or parsing the response incorrectly.

**Solution Applied:**
Fixed the `k6-tests/lib/api-client.js` file:

1. **Changed endpoint** from `/api/auth/login` to `/api/auth/signin`
2. **Improved token parsing** to handle JSON response properly
3. **Added error handling** for better debugging

**Verification:**
```bash
# Test authentication works
k6 run tests/auth-test.js

# Expected output:
✅ Authentication SUCCESS!
   Token: eyJhbGciOiJIUzI1NiJ9...
   Token parts: 3 (should be 3 for JWT)
```

**If still failing:**

1. Check backend is running:
   ```bash
   curl http://localhost:8080/api/rooms
   ```

2. Check database is up:
   ```bash
   docker ps | grep postgres
   ```

3. Test authentication manually:
   ```bash
   curl -X POST http://localhost:8080/api/auth/signin \
     -H "Content-Type: application/json" \
     -d '{"username":"testuser1","password":"Test@1234"}'
   ```
   
   Should return:
   ```json
   {
     "token": "eyJhbGci...",
     "type": "Bearer",
     "id": 1,
     "username": "testuser1",
     "email": "testuser1@test.com",
     "roles": ["ROLE_USER"]
   }
   ```

4. Check backend logs for errors:
   ```bash
   tail -f backend/logs/application.log
   ```

---

### 2. ❌ Backend Not Running

**Symptoms:**
```
❌ Error: Backend not running on http://localhost:8080
```

**Solution:**
```bash
# Terminal 1: Start database
docker-compose up -d

# Terminal 2: Start backend
cd backend
./mvnw spring-boot:run

# Wait for:
# Started BookingApplication in X.XXX seconds
```

---

### 3. ❌ Database Connection Error

**Symptoms:**
```
ERROR: Connection to localhost:5432 refused
```

**Solution:**
```bash
# Check docker is running
docker ps

# Start database
docker-compose up -d

# Verify
docker ps | grep postgres

# Check logs
docker-compose logs postgres
```

---

### 4. ❌ Redis Connection Error

**Symptoms:**
```
ERROR: Cannot connect to Redis at localhost:6379
```

**Solution:**
```bash
# Check Redis is running
docker ps | grep redis

# Restart Redis
docker-compose restart redis

# Test connection
docker exec -it <redis-container> redis-cli ping
# Should return: PONG
```

---

### 5. ❌ k6 Not Installed

**Symptoms:**
```
command not found: k6
```

**Solution:**

**macOS:**
```bash
brew install k6
k6 version
```

**Linux:**
```bash
sudo gpg --no-default-keyring --keyring /usr/share/keyrings/k6-archive-keyring.gpg \
  --keyserver hkp://keyserver.ubuntu.com:80 \
  --recv-keys C5AD17C747E3415A3642D57D77C6C491D6AC1D69
echo "deb [signed-by=/usr/share/keyrings/k6-archive-keyring.gpg] https://dl.k6.io/deb stable main" | \
  sudo tee /etc/apt/sources.list.d/k6.list
sudo apt-get update
sudo apt-get install k6
```

**Windows:**
```powershell
choco install k6
```

---

### 6. ❌ Test Thresholds Failed

**Symptoms:**
```
✗ race_lock_success_rate............: 5.00%
  ✓ { expected: rate<0.02 }
```

**Understanding:**
- This means **5% of attempts succeeded** (expected: <2%)
- Indicates **locking is not working properly**

**Solution:**

1. Check pessimistic locking is enabled:
   ```bash
   grep "PESSIMISTIC_WRITE" backend/src/main/java/com/meetingroom/booking/repository/RoomRepository.java
   ```

2. Check transaction isolation:
   ```bash
   grep "isolation" backend/src/main/resources/application.properties
   ```

3. Check lock timeout:
   ```bash
   grep "lock_timeout" backend/src/main/resources/application.properties
   ```

---

### 7. ❌ Rate Limit Errors During Tests

**Symptoms:**
```
rate_limit_hit_rate................: 95.00%
http_req_failed....................: 80.00%
```

**Understanding:**
- Rate limiting is working (good!)
- But tests are hitting limits (need adjustment)

**Solution:**

Temporarily disable rate limiting for testing:

```properties
# backend/src/main/resources/application.properties
rate.limit.enabled=false
```

Or increase limits:
```properties
rate.limit.requests=100
rate.limit.minutes=1
```

**Remember:** Re-enable for production testing!

---

### 8. ❌ Deadlock Timeout Errors

**Symptoms:**
```
deadlock_timeout_rate..............: 10.00%
http_req_duration..................: avg=8000ms
```

**Understanding:**
- Requests timing out (>5s)
- Possible deadlock or high contention

**Solution:**

1. Check lock timeout configuration:
   ```properties
   spring.jpa.properties.jakarta.persistence.lock.timeout=5000
   spring.jpa.properties.jakarta.persistence.query.timeout=10000
   ```

2. Review deadlock analysis:
   ```bash
   cat DEADLOCK-ANALYSIS.md
   ```

3. Check database locks:
   ```sql
   SELECT * FROM pg_locks WHERE NOT granted;
   ```

---

### 9. ❌ Memory/Performance Issues

**Symptoms:**
```
http_req_duration..................: avg=15s p(95)=30s
checks.............................: 50%
```

**Solution:**

1. Increase JVM memory:
   ```bash
   export MAVEN_OPTS="-Xmx2g -Xms1g"
   ./mvnw spring-boot:run
   ```

2. Increase database connections:
   ```properties
   spring.datasource.hikari.maximum-pool-size=20
   ```

3. Check Redis memory:
   ```bash
   docker stats
   ```

---

### 10. ❌ Test Users Already Exist

**Symptoms:**
```
✅ User setup complete: 0 created, 1000 existing
```

**Understanding:**
- Not an error - users already registered
- Tests will work fine

**To reset:**
```sql
-- Connect to database
docker exec -it <postgres-container> psql -U booking -d bookingdb

-- Delete test users
DELETE FROM bookings WHERE user_id IN (
  SELECT id FROM users WHERE username LIKE 'testuser%'
);
DELETE FROM users WHERE username LIKE 'testuser%';
```

---

## Debugging Tips

### Enable Verbose Logging

**k6:**
```bash
k6 run --verbose tests/auth-test.js
```

**Backend:**
```properties
logging.level.com.meetingroom.booking=DEBUG
logging.level.org.springframework.security=DEBUG
```

### Check HTTP Requests

```bash
# In k6 test, add console.log
console.log('Request:', JSON.stringify(payload));
console.log('Response:', response.body);
console.log('Status:', response.status);
```

### Monitor in Real-Time

**Terminal 1:** Backend logs
```bash
tail -f backend/logs/application.log
```

**Terminal 2:** Run test
```bash
k6 run tests/auth-test.js
```

---

## Quick Health Check

Run this to verify everything is working:

```bash
#!/bin/bash

echo "🏥 Health Check"
echo ""

# 1. Check k6
if command -v k6 &> /dev/null; then
  echo "✅ k6 installed: $(k6 version | head -n 1)"
else
  echo "❌ k6 not installed"
fi

# 2. Check Docker
if docker ps &> /dev/null; then
  echo "✅ Docker running"
  
  # Check containers
  if docker ps | grep -q postgres; then
    echo "✅ PostgreSQL running"
  else
    echo "❌ PostgreSQL not running"
  fi
  
  if docker ps | grep -q redis; then
    echo "✅ Redis running"
  else
    echo "❌ Redis not running"
  fi
else
  echo "❌ Docker not running"
fi

# 3. Check Backend
if curl -s http://localhost:8080/api/rooms > /dev/null 2>&1; then
  echo "✅ Backend running (http://localhost:8080)"
else
  echo "❌ Backend not running"
fi

# 4. Test Authentication
if k6 run --quiet tests/auth-test.js > /dev/null 2>&1; then
  echo "✅ Authentication working"
else
  echo "❌ Authentication failed"
fi

echo ""
echo "📊 Status check complete"
```

---

## Getting Help

1. **Check Logs:** Backend logs show detailed error messages
2. **Read Documentation:** See README.md and QUICKSTART.md
3. **Run Health Check:** Use the script above
4. **Test Authentication First:** `k6 run tests/auth-test.js`
5. **Start Simple:** Run one test at a time, not all at once

---

## Common API Endpoints

| Endpoint | Method | Purpose |
|----------|--------|---------|
| `/api/auth/signup` | POST | Register new user |
| `/api/auth/signin` | POST | Login (get JWT token) |
| `/api/rooms` | GET | List all rooms |
| `/api/bookings` | POST | Create booking (requires auth) |
| `/api/bookings/my` | GET | Get user's bookings (requires auth) |
| `/api/bookings/{id}` | DELETE | Cancel booking (requires auth) |
| `/api/bookings/room/{roomId}` | GET | Get room's bookings |
| `/api/dashboard` | GET | Get dashboard data (requires auth) |

All authenticated endpoints require:
```
Authorization: Bearer <JWT_TOKEN>
```

---

## File Structure Reference

```
k6-tests/
├── lib/
│   └── api-client.js          ← Core API functions (FIXED)
├── scripts/
│   └── setup-users.js         ← Create 1000 test users
├── tests/
│   ├── auth-test.js           ← Test authentication (NEW)
│   ├── race-condition-test.js ← Test pessimistic locking
│   ├── deadlock-test.js       ← Test deadlock prevention
│   ├── overlap-test.js        ← Test overlap detection
│   ├── double-booking-test.js ← Test double booking prevention
│   └── stress-test.js         ← Comprehensive stress test
├── results/                   ← Test results (auto-generated)
├── run-all-tests.sh          ← Run all tests (UPDATED)
├── README.md                  ← Full documentation (UPDATED)
├── QUICKSTART.md             ← Quick start guide (UPDATED)
├── TROUBLESHOOTING.md        ← This file (NEW)
└── CHEATSHEET.md             ← Quick reference
```

---

## Change Log

### 2025-10-29: Authentication Fix

**Problem:** K6 tests failing to obtain JWT bearer token

**Changes:**
1. Fixed `/api/auth/login` → `/api/auth/signin` in `api-client.js`
2. Improved JSON response parsing
3. Added error handling
4. Created `auth-test.js` for verification
5. Updated `run-all-tests.sh` to check auth first
6. Updated README.md and QUICKSTART.md
7. Created this TROUBLESHOOTING.md guide

**Verification:**
```bash
k6 run tests/auth-test.js
# Should show: ✅ Authentication SUCCESS!
```

---

**Last Updated:** October 29, 2025
