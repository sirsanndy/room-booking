import { sleep } from 'k6';
import { testUsers, getToken, createBooking, generateOverlappingSlots } from '../lib/api-client.js';

// Test configuration
export const options = {
  setupTimeout: '15m',
  teardownTimeout: '5m',
  scenarios: {
    overlap_test: {
      executor: 'per-vu-iterations',
      vus: 150, // 50 concurrent users
      iterations: 30,
      maxDuration: '30s',
    },
  },
  thresholds: {
    'overlap_conflict_rate': ['rate>0.9'], // More than 90% should conflict
    'http_req_duration': ['p(95)<3000'],
    'checks': ['rate>0.95'],
  },
};

const ROOM_ID = 2;
const OVERLAPPING_SLOTS = generateOverlappingSlots(3); // 3 days from now (weekday)
let slot1Success = 0;
let slot2Success = 0;
let conflicts = 0;

export function setup() {
  console.log('📅 OVERLAP BOOKING TEST');
  console.log('📋 50 users trying to book overlapping time slots');
  console.log(`🏢 Target Room: Room ${ROOM_ID}`);
  console.log(`📅 Slot 1: ${OVERLAPPING_SLOTS[0].startTime} to ${OVERLAPPING_SLOTS[0].endTime}`);
  console.log(`📅 Slot 2: ${OVERLAPPING_SLOTS[1].startTime} to ${OVERLAPPING_SLOTS[1].endTime}`);
  console.log('   (These slots overlap by 1 hour - Weekdays only)');
  console.log('✅ Expected: Only ONE time slot gets bookings\n');
  
  return {
    roomId: ROOM_ID,
    slots: OVERLAPPING_SLOTS,
    startTime: Date.now(),
  };
}

export default function (data) {
  const userIndex = (__VU - 1) % testUsers.length;
  const user = testUsers[userIndex];
  
  // Get authentication token
  const token = getToken(user.username);
  if (!token) {
    return;
  }

  // Alternate between slot 1 and slot 2
  const isSlot1 = __ITER % 2 === 0;
  const slot = isSlot1 ? data.slots[0] : data.slots[1];

  const bookingData = {
    roomId: data.roomId,
    startTime: slot.startTime,
    endTime: slot.endTime,
    title: `Overlap Test - ${isSlot1 ? 'Slot1' : 'Slot2'}`,
    description: `Testing overlap prevention for ${user.username}`,
  };

  const result = createBooking(token, bookingData);

  if (result.success) {
    if (isSlot1) {
      slot1Success++;
      console.log(`✅ Slot 1 - ${user.username}: Booking succeeded`);
    } else {
      slot2Success++;
      console.log(`✅ Slot 2 - ${user.username}: Booking succeeded`);
    }
  } else if (result.conflict) {
    conflicts++;
    console.log(`❌ ${isSlot1 ? 'Slot 1' : 'Slot 2'} - ${user.username}: Conflict (expected)`);
  } else if (result.rateLimited) {
    console.log(`⏱️ ${user.username}: Rate limited`);
  }

  sleep(0.1);
}

export function teardown(data) {
  const duration = (Date.now() - data.startTime) / 1000;
  
  console.log('\n' + '='.repeat(70));
  console.log('📊 OVERLAP BOOKING TEST RESULTS');
  console.log('='.repeat(70));
  console.log(`⏱️  Duration: ${duration.toFixed(2)}s`);
  console.log(`📊 Slot 1 Success: ${slot1Success}`);
  console.log(`📊 Slot 2 Success: ${slot2Success}`);
  console.log(`🔒 Conflicts: ${conflicts}`);
  
  console.log('\n🔍 VERIFICATION:');
  
  if (slot1Success > 0 && slot2Success > 0) {
    console.log('❌ FAIL: Both overlapping slots have bookings!');
    console.log('   🚨 CRITICAL: Overlap detection NOT working!');
  } else if (slot1Success > 0 || slot2Success > 0) {
    console.log('✅ PASS: Only one time slot got booked');
    console.log('   Overlap prevention is working correctly!');
    
    if (slot1Success > 1 || slot2Success > 1) {
      console.log(`⚠️  Multiple bookings in same slot (Slot1: ${slot1Success}, Slot2: ${slot2Success})`);
      console.log('   This is a race condition issue');
    }
  } else {
    console.log('⚠️  WARNING: No bookings succeeded');
    console.log('   Check server logs and rate limiting');
  }
  
  if (conflicts > 0) {
    console.log(`✅ ${conflicts} requests correctly rejected due to overlap/conflict`);
  }
  
  console.log('='.repeat(70));
}
