#!/bin/bash

###############################################################################
# K6 Test Suite Runner
# Runs all k6 performance tests in sequence
###############################################################################

# Don't exit on error - we want to continue all tests
set +e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Test results tracking
TOTAL_TESTS=0
PASSED_TESTS=0
FAILED_TESTS=0

# Output directory
OUTPUT_DIR="./results/$(date +%Y%m%d_%H%M%S)"
mkdir -p "$OUTPUT_DIR"

echo -e "${BLUE}╔════════════════════════════════════════════════════════════╗${NC}"
echo -e "${BLUE}║  K6 Test Suite Runner - Meeting Room Booking System       ║${NC}"
echo -e "${BLUE}╚════════════════════════════════════════════════════════════╝${NC}"
echo ""

###############################################################################
# Helper Functions
###############################################################################

run_test() {
  local test_name="$1"
  local test_file="$2"
  local output_file="$OUTPUT_DIR/$test_name.json"
  
  TOTAL_TESTS=$((TOTAL_TESTS + 1))
  
  echo -e "${YELLOW}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
  echo -e "${BLUE}🧪 Running: $test_name${NC}"
  echo -e "${YELLOW}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
  echo ""
  
  if k6 run --out json="$output_file" "$test_file"; then
    echo ""
    echo -e "${GREEN}✅ PASSED: $test_name${NC}"
    PASSED_TESTS=$((PASSED_TESTS + 1))
    return 0
  else
    echo ""
    echo -e "${RED}❌ FAILED: $test_name${NC}"
    FAILED_TESTS=$((FAILED_TESTS + 1))
    return 1
  fi
}

###############################################################################
# Pre-flight Checks
###############################################################################

echo "🔍 Pre-flight checks..."
echo ""

# Check k6 installed
if ! command -v k6 &> /dev/null; then
  echo -e "${RED}❌ Error: k6 is not installed${NC}"
  echo "Install: brew install k6"
  exit 1
fi

echo -e "${GREEN}✅ k6 installed:${NC} $(k6 version | head -n 1)"

# Check backend is running
if ! curl -s http://localhost:8080/api/rooms > /dev/null 2>&1; then
  echo -e "${RED}❌ Error: Backend not running on http://localhost:8080${NC}"
  echo "Start backend: cd ../backend && ./mvnw spring-boot:run"
  exit 1
fi

echo -e "${GREEN}✅ Backend running${NC}"

# Test authentication
echo ""
echo "🔐 Testing authentication..."
AUTH_TEST_OUTPUT=$(k6 run --quiet tests/auth-test.js 2>&1)
AUTH_EXIT_CODE=$?

if [ $AUTH_EXIT_CODE -eq 0 ]; then
  echo -e "${GREEN}✅ Authentication working${NC}"
else
  echo -e "${RED}❌ Warning: Authentication test failed${NC}"
  echo "Attempting to continue with remaining tests..."
  echo "You may see authentication errors in subsequent tests"
  echo ""
fi

echo ""

# Check test users exist
echo ""
echo "🔍 Checking test users..."
USER_CHECK=$(curl -s -X POST http://localhost:8080/api/auth/signin -H "Content-Type: application/json" -d '{"username":"testuser1","password":"Test@1234"}' 2>&1)

if echo "$USER_CHECK" | grep -q "token"; then
  echo -e "${GREEN}✅ Test users exist${NC}"
else
  echo -e "${YELLOW}⚠️  Test users not found. Setting up 300 users...${NC}"
  echo ""
  k6 run scripts/setup-users.js || echo -e "${YELLOW}⚠️  Setup had issues, continuing anyway...${NC}"
  echo ""
fi

echo ""
echo -e "${BLUE}╔════════════════════════════════════════════════════════════╗${NC}"
echo -e "${BLUE}║  Starting Test Execution                                  ║${NC}"
echo -e "${BLUE}╚════════════════════════════════════════════════════════════╝${NC}"
echo ""

###############################################################################
# Run Tests
###############################################################################

# 1. Race Condition Test
run_test "race-condition" "tests/race-condition-test.js"
echo ""
sleep 2

# 2. Deadlock Test
run_test "deadlock" "tests/deadlock-test.js"
echo ""
sleep 2

# 3. Overlap Booking Test
run_test "overlap-booking" "tests/overlap-test.js"
echo ""
sleep 2

# 4. Double Booking Test
run_test "double-booking" "tests/double-booking-test.js"
echo ""
sleep 2

# 5. Comprehensive Stress Test (Read Operations Only)
echo -e "${YELLOW}⚠️  Next: 5-minute high concurrency stress test${NC}"
echo -e "${YELLOW}   100 concurrent users × 30 requests/second = 3000+ req/s${NC}"
echo -e "${YELLOW}   Testing READ operations only (dashboard, bookings list, rooms)${NC}"
echo -e "${YELLOW}   This will take approximately 5 minutes...${NC}"
echo ""
read -p "Continue? (y/n) " -n 1 -r
echo
if [[ $REPLY =~ ^[Yy]$ ]]; then
  run_test "stress-test" "tests/stress-test.js"
else
  echo -e "${YELLOW}⏭️  Skipped: stress-test${NC}"
fi

###############################################################################
# Summary Report
###############################################################################

echo ""
echo -e "${BLUE}╔════════════════════════════════════════════════════════════╗${NC}"
echo -e "${BLUE}║  Test Execution Summary                                   ║${NC}"
echo -e "${BLUE}╚════════════════════════════════════════════════════════════╝${NC}"
echo ""
echo -e "Total Tests:   ${BLUE}$TOTAL_TESTS${NC}"
echo -e "Passed:        ${GREEN}$PASSED_TESTS${NC}"
echo -e "Failed:        ${RED}$FAILED_TESTS${NC}"
echo ""
echo -e "Results saved: ${BLUE}$OUTPUT_DIR${NC}"
echo ""

# Generate summary file
cat > "$OUTPUT_DIR/summary.txt" << EOF
K6 Test Suite - Execution Summary
Generated: $(date)

Test Results:
=============
Total Tests: $TOTAL_TESTS
Passed:      $PASSED_TESTS
Failed:      $FAILED_TESTS

Test Files:
===========
EOF

ls -lh "$OUTPUT_DIR"/*.json >> "$OUTPUT_DIR/summary.txt" 2>/dev/null || true

echo -e "${GREEN}✅ Summary saved to: $OUTPUT_DIR/summary.txt${NC}"
echo ""

###############################################################################
# Generate HTML Report (Always runs, even if tests failed)
###############################################################################

echo -e "${BLUE}╔════════════════════════════════════════════════════════════╗${NC}"
echo -e "${BLUE}║  Generating HTML Report                                   ║${NC}"
echo -e "${BLUE}╚════════════════════════════════════════════════════════════╝${NC}"
echo ""

# Check if Node.js is available
if command -v node &> /dev/null; then
  # Check if node_modules exists
  if [ ! -d "node_modules" ]; then
    echo -e "${YELLOW}📦 Installing dependencies...${NC}"
    npm install --silent || echo -e "${YELLOW}⚠️  npm install had issues, continuing...${NC}"
  fi
  
  # Generate HTML report (always attempt, even if tests failed)
  echo "📊 Generating HTML report from test results..."
  if node scripts/generate-html-report.js "$OUTPUT_DIR" "$OUTPUT_DIR/report.html" 2>&1; then
    echo ""
    echo -e "${GREEN}✅ HTML report generated successfully!${NC}"
    echo -e "${BLUE}📊 Report location: $OUTPUT_DIR/report.html${NC}"
    echo ""
    echo -e "${YELLOW}To view the report:${NC}"
    echo -e "   ${BLUE}open $OUTPUT_DIR/report.html${NC}"
    echo -e "   or"
    echo -e "   ${BLUE}file://$(pwd)/$OUTPUT_DIR/report.html${NC}"
    echo ""
    
    # Create a 'latest' symlink
    rm -f ./results/latest
    ln -sf "$(basename $OUTPUT_DIR)" ./results/latest
    echo -e "${GREEN}✅ Latest results linked: ./results/latest${NC}"
  else
    echo -e "${YELLOW}⚠️  HTML report generation had issues, but continuing...${NC}"
    echo -e "${YELLOW}   Check if JSON output files exist in $OUTPUT_DIR${NC}"
  fi
else
  echo -e "${YELLOW}⚠️  Node.js not found. Skipping HTML report generation.${NC}"
  echo -e "${YELLOW}   Install Node.js to enable HTML reports: https://nodejs.org${NC}"
fi

echo ""

# Final status (non-blocking)
if [ $FAILED_TESTS -gt 0 ]; then
  echo -e "${YELLOW}⚠️  Some tests failed, but report was generated. Review results above.${NC}"
  echo -e "${BLUE}📊 View detailed report: open $OUTPUT_DIR/report.html${NC}"
  exit 1
else
  echo -e "${GREEN}✅ All tests passed!${NC}"
  echo -e "${BLUE}📊 View detailed report: open $OUTPUT_DIR/report.html${NC}"
  exit 0
fi
