# K6 Tests - Cheat Sheet

## Quick Commands

### Setup (One-time)
```bash
brew install k6                           # Install k6
cd k6-tests
k6 run scripts/setup-users.js             # Create 1000 users (~2 min)
```

### Run Tests
```bash
# Individual tests
k6 run tests/race-condition-test.js       # 30s - Race condition
k6 run tests/deadlock-test.js             # 70s - Deadlock prevention
k6 run tests/overlap-test.js              # 30s - Overlap detection
k6 run tests/double-booking-test.js       # 60s - Double booking
k6 run tests/stress-test.js               # 10m - Full stress test

# All tests
./run-all-tests.sh                        # Run everything
```

### Reports
```bash
k6 run --out html=report.html tests/stress-test.js   # HTML report
k6 run --out json=results.json tests/stress-test.js  # JSON export
open report.html                                      # View report
```

### Custom Options
```bash
k6 run --vus 500 tests/stress-test.js                # 500 users
k6 run --duration 5m tests/stress-test.js            # 5 minutes
k6 run --iterations 100 tests/race-condition-test.js # 100 iterations
```

## Test Summary

| Test | VUs | Duration | Validates |
|------|-----|----------|-----------|
| **race-condition** | 100 | 30s | PESSIMISTIC_WRITE locking |
| **deadlock** | 200 | 70s | Lock timeout (5s) |
| **overlap** | 50 | 30s | SQL overlap query |
| **double-booking** | 100 | 60s | User booking limit |
| **stress** | 1000 | 10m | System stability |

## Understanding Results

### ✅ Success Indicators
- `✓ race_lock_success_rate: 1.00%` → Only 1 booking succeeded
- `✓ http_req_duration: p(95)<5000ms` → 95% requests < 5s
- `✓ checks: 95%` → 95% checks passed
- Exit code: `0` → All thresholds met

### ❌ Failure Indicators
- `✗ race_lock_success_rate: 15%` → Multiple bookings (BUG!)
- `✗ http_req_duration: p(95)>5000ms` → Too slow
- `✗ checks: 80%` → Too many failures
- Exit code: `1` → Thresholds violated

## Key Metrics

| Metric | Good | Warning | Critical |
|--------|------|---------|----------|
| **p95 Latency** | <2s | 2-5s | >5s |
| **Throughput** | >100/s | 50-100/s | <50/s |
| **Success Rate** | >60% | 40-60% | <40% |
| **Timeout Rate** | <1% | 1-5% | >5% |

## Troubleshooting

### Connection Refused
```bash
# Problem: Backend not running
# Fix:
cd ../backend && ./mvnw spring-boot:run
```

### Test Users Missing
```bash
# Problem: Users not created
# Fix:
k6 run scripts/setup-users.js
```

### High Failure Rate (>80%)
```bash
# Check rate limiting (30 req/min per user)
# Check database connections
# Check server resources: top
```

### Timeouts
```bash
# Check lock timeout: 5s in application.properties
# Check database: SELECT * FROM pg_locks;
# Check Redis: redis-cli monitor
```

## Monitoring

### Backend Logs
```bash
tail -f ../backend/logs/application.log | grep -E "lock|booking|error"
```

### Database
```sql
SELECT * FROM pg_locks WHERE NOT granted;               -- Locks
SELECT * FROM pg_stat_activity WHERE state = 'active'; -- Active queries
```

### Redis
```bash
redis-cli monitor | grep -E "rate|token"  # Monitor commands
redis-cli info clients                     # Connection count
```

### System
```bash
top                      # CPU/Memory
ps aux | grep k6         # k6 process
netstat -an | grep 8080  # Active connections
```

## Expected Benchmarks

### Race Condition Test
- Success rate: **1%** (1 of 100)
- p95 latency: **<2s**
- Throughput: **50-80 req/s**

### Deadlock Test
- Timeout rate: **<5%**
- p95 latency: **<3s**
- Throughput: **30-50 req/s**

### Overlap Test
- Conflict rate: **>90%**
- p95 latency: **<2s**
- Throughput: **30-40 req/s**

### Double Booking Test
- Max bookings/user: **1**
- p95 latency: **<3s**
- Throughput: **40-60 req/s**

### Stress Test (1000 Users)
- Throughput: **100-200 req/s**
- p95 latency: **<5s**
- p99 latency: **<10s**
- Success rate: **50-70%**

## CI/CD Integration

### GitHub Actions
```yaml
- run: cd k6-tests && k6 run scripts/setup-users.js
- run: cd k6-tests && ./run-all-tests.sh
```

### Jenkins
```groovy
sh 'cd k6-tests && ./run-all-tests.sh'
```

## File Structure

```
k6-tests/
├── lib/api-client.js           # Core API client
├── scripts/setup-users.js      # Create 1000 users
├── tests/
│   ├── race-condition-test.js  # Locking test
│   ├── deadlock-test.js        # Timeout test
│   ├── overlap-test.js         # Overlap test
│   ├── double-booking-test.js  # User limit test
│   └── stress-test.js          # Full stress test
├── run-all-tests.sh            # Run all tests
└── results/                    # Test results
```

## Environment Variables

```bash
export API_URL=http://localhost:8080  # Override API URL
k6 run tests/stress-test.js
```

## Test Users

- **Usernames**: `testuser1` to `testuser1000`
- **Password**: `Test@1234`
- **Created by**: `k6 run scripts/setup-users.js`

## Documentation

- **Complete guide**: `README.md`
- **Quick start**: `QUICKSTART.md`
- **Full summary**: `K6-TESTS-COMPLETE.md`
- **This cheat sheet**: `CHEATSHEET.md`

## Help

```bash
k6 --help                           # k6 help
k6 run --help                       # Run options
cat results/latest/summary.txt      # Latest results
```

## Cleanup

```bash
# Remove test bookings (use Node.js cleanup)
cd ../k8s-tests && npm run clean:bookings

# Or SQL
psql -h localhost -U booking -d booking
DELETE FROM bookings WHERE user_id IN (
  SELECT id FROM users WHERE username LIKE 'testuser%'
);
```

---

**Quick Start:** `./run-all-tests.sh` 🚀
