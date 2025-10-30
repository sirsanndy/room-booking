# K6 Test Suite Resilience Features

## Overview
The K6 test suite is designed to be resilient and continue execution even when individual tests fail, ensuring comprehensive test coverage and always generating reports.

## Key Features

### 1. **Non-Blocking Test Execution**
- Tests run sequentially but failures don't halt execution
- Script uses `set +e` instead of `set -e` to continue on errors
- Each test failure is tracked but doesn't stop the suite

### 2. **Graceful Authentication Handling**
All tests handle authentication failures gracefully:

```javascript
const token = getToken(user.username);
if (!token) {
  failedBookings++;  // Track the failure
  return;            // Skip this iteration, continue to next
}
```

**Benefits:**
- ✅ Tests continue even if some users fail to authenticate
- ✅ Failed authentication is counted and reported
- ✅ Partial test results are still valuable

### 3. **Guaranteed HTML Report Generation**
The HTML report is **always** generated, even if:
- ❌ Some tests fail
- ❌ Authentication fails
- ❌ Backend has issues
- ❌ Rate limiting is hit

**Implementation:**
```bash
# Report generation always runs (non-blocking)
if node scripts/generate-html-report.js "$OUTPUT_DIR" "$OUTPUT_DIR/report.html" 2>&1; then
  echo "✅ HTML report generated successfully!"
else
  echo "⚠️  HTML report generation had issues, but continuing..."
fi
```

### 4. **Comprehensive Error Tracking**
Each test tracks multiple failure modes:
- Token acquisition failures
- Rate limiting hits
- Timeout errors
- Conflict errors
- General failures

**Metrics Tracked:**
```javascript
let totalRequests = 0;
let successfulBookings = 0;
let failedBookings = 0;
let rateLimited = 0;
let weekendBookings = 0;
let weekdayBookings = 0;
```

### 5. **Detailed Summary Reports**
Even with failures, the suite generates:
1. **Terminal Summary** - Immediate feedback
2. **summary.txt** - Text-based summary
3. **report.html** - Full interactive HTML report
4. **JSON files** - Raw test data for each test

## Test Failure Behavior

### Scenario 1: Authentication Fails
```bash
🔐 Testing authentication...
❌ Warning: Authentication test failed
Attempting to continue with remaining tests...

✅ Tests continue
✅ Failures tracked
✅ Report generated
```

### Scenario 2: Individual Test Fails
```bash
🧪 Running: race-condition
❌ FAILED: race-condition

🧪 Running: deadlock        # ✅ Continues!
✅ PASSED: deadlock

🧪 Running: overlap-booking # ✅ Continues!
✅ PASSED: overlap-booking

📊 Generating HTML Report... # ✅ Always runs!
✅ HTML report generated successfully!
```

### Scenario 3: Backend Stops Mid-Test
```bash
🧪 Running: stress-test
... (some requests succeed)
... (backend stops)
... (remaining requests fail)

📊 Test Statistics:
  ✅ Successful: 1234
  ❌ Failed: 567
  
📊 Generating HTML Report... # ✅ Generates with partial data!
✅ HTML report generated successfully!
```

## Pre-flight Checks

The suite performs checks but handles failures gracefully:

### 1. **Backend Check**
```bash
if ! curl -s http://localhost:8080/api/rooms > /dev/null 2>&1; then
  echo "❌ Error: Backend not running"
  exit 1  # Only pre-flight checks exit early
fi
```

### 2. **User Setup Check**
```bash
if test users don't exist; then
  k6 run scripts/setup-users.js || echo "⚠️  Setup had issues, continuing anyway..."
fi
```

## Report Guarantees

The HTML report **WILL** be generated if:
- ✅ At least one test produces JSON output
- ✅ Node.js is installed
- ✅ npm dependencies are available

The report **shows**:
- All completed tests (pass/fail)
- Partial results from interrupted tests
- Detailed metrics and statistics
- Error summaries
- Performance graphs

## Best Practices

### For Test Writers:
1. Always check for `null` tokens before API calls
2. Use `return` instead of `throw` for recoverable errors
3. Track both successes and failures in metrics
4. Log meaningful error messages

### For Test Runners:
1. Review pre-flight check warnings
2. Check HTML report even if tests fail
3. Look for patterns in failures (all auth vs. sporadic)
4. Use partial results to diagnose issues

## Example Output

### Successful Run:
```
Total Tests:   5
Passed:        5
Failed:        0

✅ All tests passed!
📊 View detailed report: open ./results/20251029_143022/report.html
```

### Partial Success:
```
Total Tests:   5
Passed:        3
Failed:        2

⚠️  Some tests failed, but report was generated.
📊 View detailed report: open ./results/20251029_143022/report.html
```

## Troubleshooting

### Issue: All tests fail authentication
**Solution:** 
1. Check backend is running: `curl http://localhost:8080/api/rooms`
2. Run setup: `k6 run scripts/setup-users.js`
3. Test auth manually: `k6 run tests/auth-test.js`

### Issue: Some tests timeout
**Solution:**
- Review `setupTimeout` and `teardownTimeout` settings
- Check backend performance
- Reduce concurrent users in test configuration

### Issue: HTML report not generated
**Solution:**
1. Check Node.js: `node --version`
2. Install dependencies: `npm install`
3. Check JSON files exist: `ls -la results/latest/*.json`

## Configuration

### Timeout Settings
All tests use 15-minute timeouts:
```javascript
export const options = {
  setupTimeout: '15m',
  teardownTimeout: '5m',
};
```

### Retry Logic
Tests don't retry by default but continue on failure:
- ❌ Failed request → counted as failure, move to next
- ✅ This prevents cascading failures

## Summary

The K6 test suite prioritizes:
1. **Resilience** - Continue testing despite failures
2. **Visibility** - Always generate reports
3. **Actionability** - Clear error tracking and reporting
4. **Flexibility** - Partial results are valuable

**Result:** Even with issues, you get comprehensive test data to diagnose and fix problems! 🚀
