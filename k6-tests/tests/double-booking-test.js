import { sleep } from 'k6';
import { testUsers, getToken, createBooking, generateTimeSlot } from '../lib/api-client.js';

// Test configuration
export const options = {
  setupTimeout: '15m',
  teardownTimeout: '5m',
  scenarios: {
    double_booking_test: {
      executor: 'per-vu-iterations',
      vus: 100, // 100 concurrent users
      iterations: 5, // Each user tries 5 rooms
      maxDuration: '60s',
    },
  },
  thresholds: {
    'http_req_duration': ['p(95)<5000'],
    'checks': ['rate>0.8'],
  },
};

const ROOMS = [1, 2, 3, 4];
const TEST_SLOT = generateTimeSlot(4, 1, false); // 4 days from now, 8:00 AM UTC+7 (weekday)
const userBookings = {}; // Track bookings per user

export function setup() {
  console.log('👥 DOUBLE BOOKING TEST');
  console.log('📋 100 users each try to book ALL 5 rooms at the SAME time');
  console.log(`📅 Time Slot: ${TEST_SLOT.startTime} to ${TEST_SLOT.endTime} (Weekday only)`);
  console.log(`🏢 Rooms: ${ROOMS.length} different rooms`);
  console.log('✅ Expected: Each user can have MAX 1 booking at a time\n');
  
  return {
    rooms: ROOMS,
    timeSlot: TEST_SLOT,
    startTime: Date.now(),
  };
}

export default function (data) {
  const userIndex = (__VU - 1) % testUsers.length;
  const user = testUsers[userIndex];
  const username = user.username;
  
  // Initialize user booking count
  if (!userBookings[username]) {
    userBookings[username] = [];
  }
  
  // Get authentication token
  const token = getToken(username);
  if (!token) {
    return;
  }

  // Try to book a different room each iteration
  const roomId = data.rooms[__ITER % data.rooms.length];

  const bookingData = {
    roomId,
    startTime: data.timeSlot.startTime,
    endTime: data.timeSlot.endTime,
    title: `Double Booking Test - Room ${roomId}`,
    description: `User ${username} attempting room ${roomId}`,
  };

  const result = createBooking(token, bookingData);

  if (result.success) {
    userBookings[username].push({
      roomId,
      bookingId: result.data.id,
    });
    console.log(`✅ ${username}: Booked Room ${roomId} (Total: ${userBookings[username].length})`);
    
    // Warning if user has multiple bookings
    if (userBookings[username].length > 1) {
      console.log(`⚠️  WARNING: ${username} has ${userBookings[username].length} bookings at same time!`);
    }
  } else if (result.conflict) {
    // Check if it's a double booking prevention or room already booked
    if (result.data && result.data.includes('overlapping')) {
      console.log(`✅ ${username}: Room ${roomId} - Double booking prevented (correct)`);
    } else {
      console.log(`❌ ${username}: Room ${roomId} - Room already booked by another user`);
    }
  } else if (result.rateLimited) {
    console.log(`⏱️  ${username}: Rate limited`);
  }

  sleep(0.2);
}

export function teardown(data) {
  const duration = (Date.now() - data.startTime) / 1000;
  
  let usersWithMultiple = 0;
  let usersWithOne = 0;
  let usersWithNone = 0;
  let totalBookings = 0;

  // Analyze results
  Object.entries(userBookings).forEach(([username, bookings]) => {
    totalBookings += bookings.length;
    
    if (bookings.length > 1) {
      usersWithMultiple++;
      console.log(`❌ ${username}: ${bookings.length} bookings at same time!`);
      bookings.forEach(b => console.log(`   - Room ${b.roomId}`));
    } else if (bookings.length === 1) {
      usersWithOne++;
    } else {
      usersWithNone++;
    }
  });

  console.log('\n' + '='.repeat(70));
  console.log('📊 DOUBLE BOOKING TEST RESULTS');
  console.log('='.repeat(70));
  console.log(`⏱️  Duration: ${duration.toFixed(2)}s`);
  console.log(`📈 Total Bookings: ${totalBookings}`);
  console.log(`✅ Users with 1 booking: ${usersWithOne}`);
  console.log(`❌ Users with 2+ bookings: ${usersWithMultiple}`);
  console.log(`⚠️  Users with 0 bookings: ${usersWithNone}`);
  
  console.log('\n🔍 VERIFICATION:');
  
  if (usersWithMultiple === 0) {
    console.log('✅ PASS: No user has multiple overlapping bookings');
    console.log('   Double booking prevention is working correctly!');
  } else {
    console.log(`❌ FAIL: ${usersWithMultiple} users have multiple bookings at same time!`);
    console.log('   🚨 CRITICAL: Double booking validation NOT working!');
  }
  
  console.log('='.repeat(70));
}
