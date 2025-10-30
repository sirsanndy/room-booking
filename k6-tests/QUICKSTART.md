# K6 Quick Start Guide

## ⚡ Fast Track (5 minutes)

### Step 1: Install k6 (1 minute)

```bash
# macOS
brew install k6

# Verify
k6 version
```

### Step 2: Start Backend (30 seconds)

```bash
# Terminal 1: Start database
docker-compose up -d

# Terminal 2: Start backend
cd backend
./mvnw spring-boot:run
```

Wait for: `Started BookingApplication in X.XXX seconds`

### Step 3: Test Authentication (30 seconds) ⚠️ IMPORTANT

```bash
cd k6-tests
k6 run tests/auth-test.js
```

**Expected output:**
```
✅ Authentication SUCCESS!
   Token: eyJhbGciOiJIUzI1NiJ9...
   Token parts: 3 (should be 3 for JWT)
```

**If this fails:** Backend is not ready or not accessible. Fix before continuing.

### Step 4: Setup Test Users (2 minutes)

```bash
k6 run scripts/setup-users.js
```

Creates 1000 users: `testuser1` to `testuser1000` (password: `Test@1234`)

### Step 5: Run Tests (2 minutes)

```bash
# Quick test - Race condition (30 seconds)
k6 run tests/race-condition-test.js

# Or run all tests
./run-all-tests.sh
```

## 📊 Understanding Results

### ✅ Test Passed

```
✓ race_lock_success_rate........: 1.00%  ✓ 1    ✗ 99
✓ http_req_duration..............: p(95)=2345ms < 5000ms
```

**Meaning:** Only 1 user booked (expected), 95% of requests < 5s

### ❌ Test Failed

```
✗ race_lock_success_rate........: 15.00%  ✓ 15   ✗ 85
✗ http_req_duration..............: p(95)=6234ms > 5000ms
```

**Meaning:** Multiple users booked (bug!), requests too slow

## 🧪 Test Suite Overview

| Test | Duration | VUs | Purpose |
|------|----------|-----|---------|
| **Race Condition** | 30s | 100 | Locking works |
| **Deadlock** | 70s | 200 | No deadlocks |
| **Overlap** | 30s | 50 | Prevents overlaps |
| **Double Booking** | 60s | 100 | User validation |
| **Stress Test** | 10m | 1000 | System stability |

## 🚀 Common Commands

```bash
# Individual tests
k6 run tests/race-condition-test.js
k6 run tests/deadlock-test.js
k6 run tests/overlap-test.js
k6 run tests/double-booking-test.js
k6 run tests/stress-test.js

# All tests
./run-all-tests.sh

# HTML report
k6 run --out html=report.html tests/stress-test.js
open report.html

# Custom VUs
k6 run --vus 500 tests/stress-test.js

# Custom duration
k6 run --duration 5m tests/stress-test.js
```

## 🔧 Troubleshooting

### Backend Not Running

```
ERRO[0000] connection refused
```

**Fix:** Start backend → `cd ../backend && ./mvnw spring-boot:run`

### Test Users Missing

```
✗ login successful
```

**Fix:** Setup users → `k6 run scripts/setup-users.js`

### High Failure Rate

```
✗ http_req_failed: 80%
```

**Check:**
1. Rate limiting (30 req/min per user)
2. Database connections
3. Server resources (`top`)

### Timeouts

```
✗ http_req_duration: p(95)=15000ms
```

**Check:**
1. Lock timeout (5s configured)
2. Database slow queries
3. Redis latency

## 📈 Performance Benchmarks

Expected on modern hardware (8-core, 16GB RAM):

| Metric | Good | Warning | Critical |
|--------|------|---------|----------|
| **p95 Latency** | <2s | 2-5s | >5s |
| **Throughput** | >100 req/s | 50-100 | <50 |
| **Success Rate** | >60% | 40-60% | <40% |
| **Timeout Rate** | <1% | 1-5% | >5% |

## 🎯 Next Steps

1. **Review Results:** Check `./results/` directory
2. **Analyze Metrics:** Look for p95, p99 latency
3. **Fix Issues:** Update backend based on findings
4. **Rerun Tests:** Validate improvements
5. **CI/CD:** Integrate into pipeline (see README.md)

## 📚 Resources

- **Full README:** `README.md`
- **k6 Docs:** https://k6.io/docs/
- **Backend Logs:** `../backend/logs/application.log`
- **Database:** `psql -h localhost -U booking -d booking`

## 🆘 Quick Help

```bash
# k6 help
k6 run --help

# List test files
ls -lh tests/

# View test results
cat results/latest/summary.txt

# Clean test data
cd ../k8s-tests && npm run clean:bookings
```

---

**Ready?** → `./run-all-tests.sh` 🚀
