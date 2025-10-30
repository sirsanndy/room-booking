import { sleep, check } from 'k6';
import { 
  testUsers, 
  getToken, 
  getUserBookings,
  getRooms,
  getRoomBookings,
  getDashboard
} from '../lib/api-client.js';

// Stress Test Configuration - High Concurrent Load (Read Operations Only)
// Each user generates ~30 requests/second
// Target: 100+ concurrent users = 3000+ req/s total
export const options = {
  setupTimeout: '15m',
  teardownTimeout: '5m',
  scenarios: {
    // High concurrency scenario: 100+ users hammering the API
    high_concurrency_stress: {
      executor: 'constant-vus',
      vus: 100,              // 100 concurrent users
      duration: '5m',        // 5 minutes of sustained load
    },
  },
  thresholds: {
    // Adjusted thresholds for high load
    'http_req_duration': ['p(95)<5000', 'p(99)<10000'],
    'http_req_failed': ['rate<0.2'], // Allow up to 20% failure under extreme load
    'http_reqs': ['rate>3000'], // Target: >3000 requests per second (100 users × 30 req/s)
    
    // Specific thresholds for read operations
    'http_req_duration{name:GetDashboard}': ['p(95)<3000'],
    'http_req_duration{name:GetRooms}': ['p(95)<2000'],
    'http_req_duration{name:GetUserBookings}': ['p(95)<3000'],
    'http_req_duration{name:GetRoomBookings}': ['p(95)<3000'],
  },
};

const ROOMS = [1, 2, 3, 4];
let totalRequests = 0;

export function setup() {
  console.log('🚀 HIGH CONCURRENCY STRESS TEST - READ OPERATIONS');
  console.log('═'.repeat(70));
  console.log('📋 Test Configuration:');
  console.log('   👥 Concurrent Users: 100');
  console.log('   ⚡ Target Rate: ~30 requests/second per user');
  console.log('   📈 Total Target: 3000+ requests/second');
  console.log('   ⏱️  Duration: 5 minutes sustained load');
  console.log('   🏢 Available Rooms: 4');
  console.log('');
  console.log('🎯 Mixed Workload (Read Operations Only):');
  console.log('   📊 40% - Get Dashboard');
  console.log('   👤 25% - Get User Bookings');
  console.log('   🏢 20% - Get All Rooms');
  console.log('   📅 10% - Get Room Bookings');
  console.log('   🔄 5%  - Mixed Operations');
  console.log('');
  console.log('📖 Excluded Endpoints:');
  console.log('   ❌ Create/Cancel Bookings');
  console.log('   ❌ Authentication/Signup');
  console.log('');
  console.log('═'.repeat(70));
  console.log('🚀 Starting high concurrency test...\n');
  
  return {
    rooms: ROOMS,
    startTime: Date.now(),
  };
}

export default function (data) {
  totalRequests++;
  
  const userIndex = (__VU - 1) % testUsers.length;
  const user = testUsers[userIndex];
  
  // Get authentication token
  const token = getToken(user.username);
  if (!token) {
    return;  // Skip this iteration if token not available
  }

  // High concurrency: Fire 30 requests rapidly (no sleep between requests)
  // This achieves ~30 requests/second per user
  for (let i = 0; i < 30; i++) {
    const action = Math.random();

    if (action < 0.4) {
      // 40% - Get dashboard
      performGetDashboard(token, user.username);
    } else if (action < 0.65) {
      // 25% - Get user bookings
      performGetUserBookings(token, user.username);
    } else if (action < 0.85) {
      // 20% - Get all rooms
      performGetRooms(token);
    } else if (action < 0.95) {
      // 10% - Get room bookings
      performGetRoomBookings(token, data);
    } else {
      // 5% - Mixed: Dashboard + User Bookings + Rooms (rapid fire)
      performGetDashboard(token, user.username);
      performGetUserBookings(token, user.username);
      performGetRooms(token);
    }
  }

  // Small sleep after batch to achieve ~20 req/s average (50ms per request)
  //sleep(1);
}

function performGetDashboard(token, username) {
  const dashboard = getDashboard(token);
  
  check(dashboard, {
    'dashboard has upcomingBookings': (d) => d && d.upcomingBookings !== undefined,
  });
}

function performGetUserBookings(token, username) {
  const bookings = getUserBookings(token);
  
  check(bookings, {
    'user bookings is array': (b) => Array.isArray(b),
  });
}

function performGetRooms(token) {
  const rooms = getRooms(token);
  
  check(rooms, {
    'rooms retrieved': (r) => Array.isArray(r) && r.length > 0,
  });
}

function performGetRoomBookings(token, data) {
  const roomId = data.rooms[Math.floor(Math.random() * data.rooms.length)];
  const bookings = getRoomBookings(token, roomId);
  
  check(bookings, {
    'room bookings is array': (b) => Array.isArray(b),
  });
}

export function teardown(data) {
  const duration = (Date.now() - data.startTime) / 1000;
  const throughput = totalRequests / duration;
  
  console.log('\n' + '='.repeat(70));
  console.log('📊 STRESS TEST RESULTS - READ OPERATIONS');
  console.log('='.repeat(70));
  console.log(`⏱️  Total Duration: ${duration.toFixed(2)}s`);
  console.log(`📈 Total Requests: ${totalRequests}`);
  console.log(`⚡ Throughput: ${throughput.toFixed(2)} req/s`);
  console.log('');
  console.log('📊 Request Distribution:');
  console.log(`  📊 Dashboard Requests: 40%`);
  console.log(`  👤 User Bookings: 25%`);
  console.log(`  🏢 Rooms List: 20%`);
  console.log(`  📅 Room Bookings: 10% (4 rooms)`);
  console.log(`  🔄 Mixed Operations: 5%`);
  console.log('');
  
  console.log('🔍 ANALYSIS:');
  
  if (throughput > 3000) {
    console.log(`✅ Excellent throughput: ${throughput.toFixed(2)} req/s (target: 3000+ req/s)`);
  } else if (throughput > 2000) {
    console.log(`✅ Good throughput: ${throughput.toFixed(2)} req/s (target: 3000+ req/s)`);
  } else {
    console.log(`⚠️  Low throughput: ${throughput.toFixed(2)} req/s (target: 3000+ req/s)`);
  }
  
  console.log('');
  console.log('💡 Recommendations:');
  console.log('  - Review k6 HTML report for detailed metrics');
  console.log('  - Check backend logs for errors and performance issues');
  console.log('  - Monitor database connection pool usage');
  console.log('  - Monitor Redis cache hit/miss rates');
  console.log('  - Review application CPU and memory usage');
  console.log('  - Note: This test only exercises READ operations');
  console.log('  - For write operations, see race-condition, deadlock, overlap, and double-booking tests');
  
  console.log('='.repeat(70));
}
