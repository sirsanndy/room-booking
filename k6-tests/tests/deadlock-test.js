import { sleep } from 'k6';
import { testUsers, getToken, createBooking, generateTimeSlot } from '../lib/api-client.js';

// Test configuration - High concurrency to test deadlock
export const options = {
  setupTimeout: '15m',
  teardownTimeout: '5m',
  scenarios: {
    deadlock_test: {
      executor: 'ramping-vus',
      startVUs: 0,
      stages: [
        { duration: '10s', target: 50 },   // Ramp up to 50 users
        { duration: '20s', target: 200 },  // Ramp up to 200 users
        { duration: '30s', target: 200 },  // Stay at 200 users
        { duration: '10s', target: 0 },    // Ramp down
      ],
    },
  },
  thresholds: {
    'deadlock_timeout_rate': ['rate<0.05'], // Less than 5% timeout
    'http_req_duration': ['p(95)<5000'], // 95% under 5 seconds (lock timeout)
    'http_req_duration': ['p(99)<8000'], // 99% under 8 seconds
    'http_req_failed': ['rate<0.8'], // Less than 80% failure
    'booking_duration': ['p(95)<5000'], // 95% booking requests under 5s
  },
};

const ROOMS = [1, 2, 3, 4];
let timeoutCount = 0;
let successCount = 0;
let totalRequests = 0;

export function setup() {
  console.log('🔒 DEADLOCK TEST');
  console.log('📋 200 users booking multiple rooms with high concurrency');
  console.log(`🏢 Testing ${ROOMS.length} rooms`);
  console.log('✅ Expected: No timeouts, all requests complete within 5 seconds\n');
  
  return { 
    rooms: ROOMS,
    startTime: Date.now(),
  };
}

export default function (data) {
  totalRequests++;
  
  // Get a user based on VU number
  const userIndex = (__VU - 1) % testUsers.length;
  const user = testUsers[userIndex];
  
  // Get authentication token
  const token = getToken(user.username);
  if (!token) {
    return;
  }

  // Select a room (distribute load across rooms)
  const roomId = data.rooms[totalRequests % data.rooms.length];
  
  // Generate unique time slot for this request (weekdays only)
  // Updated to use 0-13 range for 7 AM - 9 PM window
  const hourOffset = Math.floor(totalRequests / data.rooms.length) % 14;
  const daysFromNow = 2 + Math.floor(totalRequests / (data.rooms.length * 14));
  const timeSlot = generateTimeSlot(daysFromNow, hourOffset, false);

  // Create booking
  const bookingData = {
    roomId,
    startTime: timeSlot.startTime,
    endTime: timeSlot.endTime,
    title: `Deadlock Test - Room ${roomId}`,
    description: `User ${user.username} booking room ${roomId}`,
  };

  const result = createBooking(token, bookingData);

  if (result.success) {
    successCount++;
    if (successCount % 20 === 0) {
      console.log(`✅ ${successCount} bookings succeeded...`);
    }
  } else if (result.timeout) {
    timeoutCount++;
    console.log(`⚠️  ${user.username}: TIMEOUT detected! (>${result.timeout}ms)`);
  } else if (result.conflict) {
    // Expected - room might be booked by another user
  } else if (result.rateLimited) {
    console.log(`⏱️  ${user.username}: Rate limited`);
  }

  // Random delay to simulate real user behavior
  sleep(Math.random() * 2);
}

export function teardown(data) {
  const duration = (Date.now() - data.startTime) / 1000;
  const avgResponseTime = duration / totalRequests * 1000;
  
  console.log('\n' + '='.repeat(70));
  console.log('📊 DEADLOCK TEST RESULTS');
  console.log('='.repeat(70));
  console.log(`⏱️  Duration: ${duration.toFixed(2)}s`);
  console.log(`📈 Total Requests: ${totalRequests}`);
  console.log(`✅ Successful Bookings: ${successCount}`);
  console.log(`⏱️  Timeouts: ${timeoutCount}`);
  console.log(`📊 Timeout Rate: ${((timeoutCount / totalRequests) * 100).toFixed(2)}%`);
  console.log(`⚡ Avg Response Time: ${avgResponseTime.toFixed(0)}ms`);
  
  console.log('\n🔍 VERIFICATION:');
  
  if (timeoutCount === 0) {
    console.log('✅ PASS: No timeouts detected');
    console.log('   All requests completed within lock timeout (5 seconds)');
  } else {
    console.log(`⚠️  WARNING: ${timeoutCount} timeout errors detected`);
    console.log('   This might indicate deadlock or high lock contention');
    console.log('   Check application logs for "deadlock detected" or "lock timeout"');
  }
  
  if (avgResponseTime < 5000) {
    console.log(`✅ Average response time ${avgResponseTime.toFixed(0)}ms is acceptable`);
  } else {
    console.log(`⚠️  WARNING: Average response time ${avgResponseTime.toFixed(0)}ms > 5000ms`);
    console.log('   High response times indicate lock contention');
  }
  
  console.log('='.repeat(70));
}
