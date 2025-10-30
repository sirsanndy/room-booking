# K6 Load Testing Suite - Meeting Room Booking System

## Overview

Professional-grade load testing suite using **k6** to validate system performance, concurrency control, and stress handling with **1000+ concurrent users**.

## Why k6?

k6 is superior for performance testing compared to Node.js-based solutions:

- ✅ **High Performance**: Written in Go, handles 1000+ concurrent users efficiently
- ✅ **Accurate Metrics**: Precise timing, percentiles (p95, p99), and throughput
- ✅ **Built-in Thresholds**: Pass/fail criteria for automated testing
- ✅ **Realistic Load**: Proper VU (Virtual User) simulation
- ✅ **Professional Reports**: HTML, JSON, and real-time metrics
- ✅ **CI/CD Ready**: Exit codes, JSON output, cloud integration

## Test Suite

### 1. 🏁 Race Condition Test
- **100 users** simultaneously book the **SAME room** at the **SAME time**
- **Validates**: PESSIMISTIC_WRITE locking
- **Expected**: Only 1 booking succeeds
- **Duration**: ~30 seconds

### 2. 🔒 Deadlock Test
- **200 users** with **ramping load** (0 → 200 over 30s)
- **Validates**: Lock timeout, no circular dependencies
- **Expected**: Zero timeouts, all complete < 5s
- **Duration**: ~70 seconds (includes ramp up/down)

### 3. 📅 Overlap Booking Test
- **50 users** try **overlapping time slots**
- **Validates**: SQL overlap detection
- **Expected**: Only ONE slot gets bookings
- **Duration**: ~30 seconds

### 4. 👥 Double Booking Test
- **100 users** each try **5 rooms** simultaneously
- **Validates**: User validation prevents multiple bookings
- **Expected**: Each user has ≤ 1 booking at a time
- **Duration**: ~60 seconds

### 5. 🚀 Comprehensive Stress Test
- **0 → 1000 users** gradual ramp up
- **10 minutes** sustained load
- **Mixed workload**: 30% bookings, 70% reads/dashboard/cancellations
- **Validates**: System stability under production-like load
- **Expected**: >100 req/s, <50% failure rate

## Prerequisites

### 1. Install k6

**macOS:**
```bash
brew install k6
```

**Linux:**
```bash
sudo gpg -k
sudo gpg --no-default-keyring --keyring /usr/share/keyrings/k6-archive-keyring.gpg --keyserver hkp://keyserver.ubuntu.com:80 --recv-keys C5AD17C747E3415A3642D57D77C6C491D6AC1D69
echo "deb [signed-by=/usr/share/keyrings/k6-archive-keyring.gpg] https://dl.k6.io/deb stable main" | sudo tee /etc/apt/sources.list.d/k6.list
sudo apt-get update
sudo apt-get install k6
```

**Windows:**
```powershell
choco install k6
```

**Or download binary:** https://k6.io/docs/get-started/installation/

### 2. Verify Installation

```bash
k6 version
# Should show: k6 v0.xx.x
```

### 3. Backend Running

```bash
cd ../backend
./mvnw spring-boot:run
```

Verify: `http://localhost:8080/api/rooms`

### 4. Database & Redis

```bash
docker-compose up -d
```

## Quick Start

### Step 0: Test Authentication (Recommended First)

Before running the full test suite, verify that authentication works correctly:

```bash
# Test authentication endpoint and token acquisition
k6 run tests/auth-test.js

# Expected output:
# ✅ Authentication SUCCESS!
# Token: eyJhbGciOiJIUzI1NiJ9...
# Token parts: 3 (should be 3 for JWT)
```

**If authentication fails**, check:
1. Backend is running on `http://localhost:8080`
2. Database is up and accessible
3. Check backend logs for errors
4. Verify `/api/auth/signin` endpoint is accessible

### Step 1: Setup Test Users

Create 1000 test users:

```bash
k6 run scripts/setup-users.js
```

**Expected output:**
```
🚀 Setting up 1000 test users...
✅ Processed 100 users...
✅ Processed 200 users...
...
✅ User setup complete: 1000 created, 0 existing
🔑 Logging in all users...
✅ Login complete: 1000 users authenticated
```

**Duration:** ~2-3 minutes (k6 is much faster than Node.js)

### Step 2: Run Individual Tests

```bash
# Race condition test
k6 run tests/race-condition-test.js

# Deadlock test
k6 run tests/deadlock-test.js

# Overlap booking test
k6 run tests/overlap-test.js

# Double booking test
k6 run tests/double-booking-test.js

# Comprehensive stress test
k6 run tests/stress-test.js
```

### Step 3: Run All Tests

```bash
./run-all-tests.sh
```

## Test Reports

### Basic Output

k6 provides real-time metrics:

```
running (0m30.0s), 100/100 VUs, 100 complete and 0 interrupted iterations

     ✓ booking response received
     ✓ race_lock_success_rate........: 1.00%  ✓ 1    ✗ 99  
     
     data_received..................: 45 kB   1.5 kB/s
     data_sent......................: 32 kB   1.1 kB/s
     http_req_blocked...............: avg=1.2ms    p(95)=3.5ms   p(99)=5.1ms
     http_req_duration..............: avg=234ms    p(95)=450ms   p(99)=890ms
     http_reqs......................: 100     3.33/s
     iteration_duration.............: avg=2.5s     p(95)=3.2s    p(99)=3.8s
     vus............................: 100     min=100 max=100
```

### HTML Report

Generate detailed HTML report:

```bash
k6 run --out html=report.html tests/stress-test.js
```

Open `report.html` in browser for:
- Interactive graphs
- Request timeline
- Error details
- Performance breakdown

### JSON Export

Export for analysis:

```bash
k6 run --out json=results.json tests/stress-test.js
```

### InfluxDB + Grafana (Advanced)

```bash
k6 run --out influxdb=http://localhost:8086/k6 tests/stress-test.js
```

Real-time dashboard in Grafana.

## Configuration

### Environment Variables

```bash
# Override API URL
export API_URL=http://your-server:8080

# Run test
k6 run tests/race-condition-test.js
```

### Custom Options

Edit test files to modify:

```javascript
export const options = {
  scenarios: {
    stress_test: {
      executor: 'ramping-vus',
      stages: [
        { duration: '30s', target: 100 },
        // Modify stages here
      ],
    },
  },
  thresholds: {
    'http_req_duration': ['p(95)<5000'], // Modify thresholds
  },
};
```

## Understanding Results

### Pass/Fail Criteria

k6 uses **thresholds** for automatic pass/fail:

```javascript
thresholds: {
  'http_req_duration': ['p(95)<5000'],  // FAIL if 95th percentile > 5s
  'http_req_failed': ['rate<0.5'],      // FAIL if >50% requests fail
  'checks': ['rate>0.95'],               // FAIL if <95% checks pass
}
```

**Exit codes:**
- `0` = All thresholds passed ✅
- `1` = Some thresholds failed ❌

### Key Metrics

| Metric | Description | Good Value |
|--------|-------------|------------|
| `http_req_duration` | Request latency | p95 < 2s |
| `http_reqs` | Requests per second | >100 req/s |
| `http_req_failed` | Failure rate | <10% |
| `vus` | Virtual users (concurrent) | As designed |
| `iteration_duration` | Full iteration time | Depends on test |

### Custom Metrics

We track additional metrics:

- `race_lock_success_rate` - Should be ~1% (1 of 100)
- `overlap_conflict_rate` - Should be >90%
- `deadlock_timeout_rate` - Should be <5%
- `rate_limit_hit_rate` - Should be <30%
- `booking_duration` - Booking-specific latency

## Advanced Usage

### Cloud Testing (k6 Cloud)

```bash
k6 cloud tests/stress-test.js
```

Runs test in k6 Cloud with global distribution.

### CI/CD Integration

#### GitHub Actions

```yaml
- name: Run k6 Tests
  run: |
    k6 run --summary-export=summary.json tests/stress-test.js
  
- name: Check Results
  run: |
    if [ $? -ne 0 ]; then
      echo "Performance tests failed!"
      exit 1
    fi
```

#### Jenkins

```groovy
stage('Performance Tests') {
  steps {
    sh 'k6 run --out json=results.json tests/stress-test.js'
    archiveArtifacts 'results.json'
  }
}
```

### Distributed Testing

Run tests from multiple machines:

```bash
# Machine 1
k6 run --vus 500 tests/stress-test.js

# Machine 2
k6 run --vus 500 tests/stress-test.js
```

### Custom Scenarios

Create custom test scenarios:

```javascript
export const options = {
  scenarios: {
    // Constant load
    constant_load: {
      executor: 'constant-vus',
      vus: 100,
      duration: '5m',
    },
    
    // Spike test
    spike: {
      executor: 'ramping-vus',
      stages: [
        { duration: '10s', target: 0 },
        { duration: '1s', target: 1000 },  // Sudden spike
        { duration: '30s', target: 1000 },
        { duration: '10s', target: 0 },
      ],
    },
  },
};
```

## Performance Benchmarks

Expected performance on modern hardware (8-core CPU, 16GB RAM):

| Test | VUs | Duration | Throughput | p95 Latency | Success Rate |
|------|-----|----------|------------|-------------|--------------|
| Race Condition | 100 | 30s | 50-80 req/s | <2s | 1% (expected) |
| Deadlock | 200 | 70s | 30-50 req/s | <3s | 60-80% |
| Overlap | 50 | 30s | 30-40 req/s | <2s | 2-4% |
| Double Booking | 100 | 60s | 40-60 req/s | <3s | 20-40% |
| Stress Test | 1000 | 10m | 100-200 req/s | <5s | 50-70% |

## Troubleshooting

### Issue: Connection Refused

```
ERRO[0000] GoError: Get "http://localhost:8080/api/rooms": dial tcp connect: connection refused
```

**Solution:** Start backend server

### Issue: Too Many Open Files

```
ERRO[0000] too many open files
```

**Solution:**
```bash
# macOS/Linux
ulimit -n 10000
```

### Issue: High Failure Rate

**Check:**
1. Rate limiting (30 req/min per user)
2. Database connection pool size
3. Server CPU/memory usage
4. Network latency

### Issue: Timeouts

**Check:**
1. Lock timeout configuration (5s)
2. Database query performance
3. Redis latency
4. Server resource exhaustion

## Monitoring During Tests

### Backend Logs

```bash
tail -f ../backend/logs/application.log | grep -E "lock|booking|error"
```

### Database

```sql
-- PostgreSQL - Active locks
SELECT * FROM pg_locks WHERE NOT granted;

-- PostgreSQL - Active queries
SELECT * FROM pg_stat_activity WHERE state = 'active';
```

### Redis

```bash
redis-cli monitor | grep -E "rate|token"
```

### System Resources

```bash
# CPU and Memory
top

# k6 resource usage
ps aux | grep k6
```

## Comparison: k6 vs Node.js Tests

| Feature | k6 | Node.js (k8s-tests) |
|---------|----|--------------------|
| **Max VUs** | 10,000+ | ~500 |
| **Performance** | Native Go | JavaScript (slower) |
| **Accuracy** | ±1ms | ±10ms |
| **Metrics** | Built-in (p95, p99) | Custom tracking |
| **Thresholds** | Automatic pass/fail | Manual verification |
| **Reports** | HTML, JSON, InfluxDB | Console output |
| **CI/CD** | Native support | Requires scripting |
| **Use Case** | Performance testing | Functional testing |

**Recommendation:** Use both!
- **Node.js tests**: Functional validation, integration testing
- **k6 tests**: Performance testing, stress testing, SLA validation

## Best Practices

1. **Start Small**: Begin with 10 VUs, gradually increase
2. **Monitor Resources**: Watch CPU, memory, database connections
3. **Set Realistic Thresholds**: Based on SLA requirements
4. **Run Regularly**: Part of CI/CD pipeline
5. **Analyze Trends**: Track performance over time
6. **Test Production-Like**: Use production data volumes
7. **Warm Up**: Allow system to warm up caches

## Support

### Documentation

- k6 Official: https://k6.io/docs/
- API Reference: https://k6.io/docs/javascript-api/
- Examples: https://k6.io/docs/examples/

### Issues

1. Check k6 logs for errors
2. Review backend application logs
3. Check database and Redis logs
4. Monitor system resources

## Cleanup

k6 tests are stateless and cleanup automatically. However, test bookings remain in database:

```bash
# Use Node.js cleanup script
cd ../k8s-tests
npm run clean:bookings
```

Or SQL:

```sql
DELETE FROM bookings WHERE user_id IN (
  SELECT id FROM users WHERE username LIKE 'testuser%'
);
```

## License

MIT

---

**Ready to test?**

```bash
k6 run scripts/setup-users.js    # Setup once
k6 run tests/stress-test.js      # Run stress test
```

Good luck! 🚀
