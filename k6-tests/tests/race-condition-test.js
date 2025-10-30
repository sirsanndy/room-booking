import { sleep } from 'k6';
import { testUsers, getToken, createBooking, cancelBooking, generateTimeSlot } from '../lib/api-client.js';

// Test configuration
export const options = {
  setupTimeout: '15m',
  teardownTimeout: '5m',
  scenarios: {
    race_condition: {
      executor: 'per-vu-iterations',
      vus: 150, // 100 concurrent users
      iterations: 30, // 100 total iterations (1 per user)
      maxDuration: '30s',
    },
  },
  thresholds: {
    'race_lock_success_rate': ['rate<0.02'], // Less than 2% success rate (ideally 1%)
    'http_req_duration': ['p(95)<5000'], // 95% of requests under 5 seconds
    'http_req_failed': ['rate<0.99'], // Less than 99% failure (we expect 99% to fail)
    'checks': ['rate>0.95'], // 95% of checks pass
  },
};

// Shared test data
const ROOM_ID = 1;
const TEST_SLOT = generateTimeSlot(1, 0, false); // Tomorrow at 7:00 AM UTC+7 (weekday only)
let successfulBookingId = null;
let successCount = 0;

export function setup() {
  console.log('🏁 RACE CONDITION TEST');
  console.log('📋 100 users simultaneously booking the SAME room at the SAME time');
  console.log(`📅 Time Slot: ${TEST_SLOT.startTime} to ${TEST_SLOT.endTime} (Weekday only)`);
  console.log(`🏢 Target Room: Room ${ROOM_ID}`);
  console.log('✅ Expected: Only 1 booking succeeds, 99 get conflicts\n');
  
  return { 
    roomId: ROOM_ID, 
    timeSlot: TEST_SLOT,
    startTime: Date.now(),
  };
}

export default function (data) {
  // Get a random user
  const userIndex = __VU - 1; // Virtual User index (0-99)
  const user = testUsers[userIndex % testUsers.length];
  
  // Get authentication token
  const token = getToken(user.username);
  if (!token) {
    console.error(`❌ Failed to get token for ${user.username}`);
    return;
  }

  // Attempt to create booking
  const bookingData = {
    roomId: data.roomId,
    startTime: data.timeSlot.startTime,
    endTime: data.timeSlot.endTime,
    title: 'Race Condition Test Booking',
    description: `User ${user.username} testing race condition`,
  };

  const result = createBooking(token, bookingData);

  if (result.success) {
    successCount++;
    successfulBookingId = result.data.id;
    console.log(`✅ ${user.username}: BOOKING SUCCEEDED! (Booking ID: ${result.data.id})`);
  } else if (result.conflict) {
    console.log(`❌ ${user.username}: Conflict - Room already booked (expected)`);
  } else if (result.rateLimited) {
    console.log(`⏱️ ${user.username}: Rate limited (HTTP 429)`);
  } else {
    console.log(`❌ ${user.username}: Failed with status ${result.status}`);
  }

  // Small delay
  sleep(0.1);
}

export function teardown(data) {
  const duration = (Date.now() - data.startTime) / 1000;
  
  console.log('\n' + '='.repeat(70));
  console.log('📊 RACE CONDITION TEST RESULTS');
  console.log('='.repeat(70));
  console.log(`⏱️  Duration: ${duration.toFixed(2)}s`);
  console.log(`✅ Successful Bookings: ${successCount}`);
  console.log(`❌ Failed Bookings: ${100 - successCount}`);
  
  if (successCount === 1) {
    console.log('\n✅ PASS: Exactly 1 booking succeeded');
    console.log('   Race condition prevention is working correctly!');
  } else if (successCount > 1) {
    console.log(`\n❌ FAIL: ${successCount} bookings succeeded!`);
    console.log('   🚨 CRITICAL: Pessimistic locking is NOT working!');
  } else {
    console.log('\n⚠️  WARNING: No bookings succeeded');
    console.log('   Check server logs for errors');
  }
  console.log('='.repeat(70));
  
  // Cleanup: Cancel the successful booking if any
  if (successfulBookingId && successCount === 1) {
    console.log('\n🧹 Cleaning up test booking...');
    // Note: Cleanup would require finding which user succeeded
    // In production, use a cleanup script
  }
}
