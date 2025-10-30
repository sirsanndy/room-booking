import http from 'k6/http';
import { check, group, sleep } from 'k6';
import { SharedArray } from 'k6/data';
import { Counter, Rate, Trend } from 'k6/metrics';

// Configuration
const BASE_URL = __ENV.API_URL || 'http://localhost:8080';
const TOTAL_USERS = 300;
const REQUEST_TIMEOUT = '15m'; // 15 minutes timeout

// Custom Metrics
const raceLockSuccessRate = new Rate('race_lock_success_rate');
const overlapConflictRate = new Rate('overlap_conflict_rate');
const deadlockTimeoutRate = new Rate('deadlock_timeout_rate');
const rateLimitHitRate = new Rate('rate_limit_hit_rate');
const bookingDuration = new Trend('booking_duration');
const authTokenCache = new Counter('auth_token_cache_hits');

// Shared test users data
const testUsers = new SharedArray('users', function () {
  const users = [];
  for (let i = 1; i <= TOTAL_USERS; i++) {
    users.push({
      username: `testuser${i}`,
      password: 'Test@1234',
      email: `testuser${i}@test.com`,
      fullName: `Test User ${i}`,
    });
  }
  return users;
});

// Token cache (shared across VUs using execution context)
let tokenCache = {};

/**
 * Register a user
 */
export function registerUser(user) {
  const payload = JSON.stringify({
    username: user.username,
    email: user.email,
    password: user.password,
    fullName: user.fullName,
  });

  const params = {
    headers: {
      'Content-Type': 'application/json',
    },
    tags: { name: 'RegisterUser' },
    timeout: REQUEST_TIMEOUT,
  };

  const response = http.post(`${BASE_URL}/api/auth/signup`, payload, params);
  
  const success = check(response, {
    'user registered': (r) => r.status === 200 || r.status === 400, // 400 = already exists
  });

  return success;
}

/**
 * Login and get JWT token
 */
export function login(username, password) {
  const payload = JSON.stringify({
    username: username,
    password: password,
  });

  const params = {
    headers: {
      'Content-Type': 'application/json',
    },
    tags: { name: 'Login' },
    timeout: REQUEST_TIMEOUT,
  };

  const response = http.post(`${BASE_URL}/api/auth/signin`, payload, params);
  
  const success = check(response, {
    'login successful': (r) => r.status === 200,
    'token received': (r) => {
      if (r.status === 200) {
        try {
          const body = JSON.parse(r.body);
          return body.token !== undefined && body.token !== null;
        } catch (e) {
          console.error('Failed to parse login response:', e);
          return false;
        }
      }
      return false;
    },
  });

  if (success && response.status === 200) {
    try {
      const body = JSON.parse(response.body);
      const token = body.token;
      if (token) {
        tokenCache[username] = token;
        return token;
      }
    } catch (e) {
      console.error('Failed to extract token:', e);
    }
  }

  return null;
}

/**
 * Get token from cache or login
 */
export function getToken(username, password = 'Test@1234') {
  if (tokenCache[username]) {
    authTokenCache.add(1);
    return tokenCache[username];
  }
  
  return login(username, password);
}

/**
 * Create a booking
 */
export function createBooking(token, bookingData) {
  const payload = JSON.stringify(bookingData);

  const params = {
    headers: {
      'Content-Type': 'application/json',
      'Authorization': `Bearer ${token}`,
    },
    tags: { name: 'CreateBooking' },
    timeout: REQUEST_TIMEOUT,
  };

  const startTime = Date.now();
  const response = http.post(`${BASE_URL}/api/bookings`, payload, params);
  const duration = Date.now() - startTime;
  
  bookingDuration.add(duration);

  const result = {
    success: response.status === 200,
    status: response.status,
    conflict: response.status === 409 || (response.body && response.body.includes('already booked')),
    rateLimited: response.status === 429,
    timeout: duration > 5000,
    data: response.status === 200 ? response.json() : null,
  };

  // Track metrics
  if (result.rateLimited) {
    rateLimitHitRate.add(1);
  }
  if (result.timeout) {
    deadlockTimeoutRate.add(1);
  }
  if (result.conflict) {
    overlapConflictRate.add(1);
  }

  check(response, {
    'booking response received': (r) => r.status !== 0,
  });

  return result;
}

/**
 * Cancel a booking
 */
export function cancelBooking(token, bookingId) {
  const params = {
    headers: {
      'Authorization': `Bearer ${token}`,
    },
    tags: { name: 'CancelBooking' },
    timeout: REQUEST_TIMEOUT,
  };

  const response = http.del(`${BASE_URL}/api/bookings/${bookingId}`, null, params);
  
  return {
    success: response.status === 200,
    status: response.status,
  };
}

/**
 * Get user's bookings
 */
export function getUserBookings(token) {
  const params = {
    headers: {
      'Authorization': `Bearer ${token}`,
    },
    tags: { name: 'GetUserBookings' },
    timeout: REQUEST_TIMEOUT,
  };

  const response = http.get(`${BASE_URL}/api/bookings/my-bookings`, params);
  
  check(response, {
    'bookings retrieved': (r) => r.status === 200,
  });

  return response.status === 200 ? response.json() : [];
}

/**
 * Get all rooms
 */
export function getRooms(token) {
  const params = {
    headers: {
      'Content-Type': 'application/json',
      'Authorization': `Bearer ${token}`,
    },
    tags: { name: 'GetRooms' },
    timeout: REQUEST_TIMEOUT,
  };

  const response = http.get(`${BASE_URL}/api/rooms`, params);
  
  check(response, {
    'rooms retrieved': (r) => r.status === 200,
  });

  return response.status === 200 ? response.json() : [];
}

/**
 * Get room bookings
 */
export function getRoomBookings(token, roomId) {
  const params = {
    headers: {
      'Authorization': `Bearer ${token}`,
    },
    tags: { name: 'GetRoomBookings' },
    timeout: REQUEST_TIMEOUT,
  };

  const response = http.get(`${BASE_URL}/api/bookings/room/${roomId}`, params);
  
  check(response, {
    'room bookings retrieved': (r) => r.status === 200,
  });

  return response.status === 200 ? response.json() : [];
}

/**
 * Get dashboard data
 */
export function getDashboard(token) {
  const params = {
    headers: {
      'Authorization': `Bearer ${token}`,
    },
    tags: { name: 'GetDashboard' },
    timeout: REQUEST_TIMEOUT,
  };

  const response = http.get(`${BASE_URL}/api/dashboard`, params);
  
  check(response, {
    'dashboard retrieved': (r) => r.status === 200,
  });

  return response.status === 200 ? response.json() : null;
}

/**
 * Get next weekday (Monday-Friday) from a given date
 */
function getNextWeekday(date) {
  const day = date.getDay();
  let daysToAdd = 1;
  
  // If Saturday (6), add 2 days to get to Monday
  if (day === 6) {
    daysToAdd = 2;
  }
  // If Sunday (0), add 1 day to get to Monday
  else if (day === 0) {
    daysToAdd = 1;
  }
  // If Friday (5), add 3 days to get to Monday
  else if (day === 5 && daysToAdd === 1) {
    daysToAdd = 3;
  }
  
  date.setDate(date.getDate() + daysToAdd);
  return date;
}

/**
 * Generate time slot for booking (weekdays only by default)
 * Time slots between 7 AM - 9 PM UTC+7 (Asia/Jakarta)
 * @param {number} daysFromNow - Days from now
 * @param {number} hourOffset - Hour offset from 7 AM (0-13 for 7 AM to 8 PM)
 * @param {boolean} allowWeekend - Whether to allow weekend bookings (default: false)
 */
export function generateTimeSlot(daysFromNow = 1, hourOffset = 0, allowWeekend = false) {
  // Create date in UTC+7 timezone
  const start = new Date();
  start.setDate(start.getDate() + daysFromNow);
  
  // Skip weekends unless explicitly allowed
  if (!allowWeekend) {
    const day = start.getDay();
    // If Saturday (6), move to Monday
    if (day === 6) {
      start.setDate(start.getDate() + 2);
    }
    // If Sunday (0), move to Monday
    else if (day === 0) {
      start.setDate(start.getDate() + 1);
    }
  }
  
  // Set hour between 7 AM and 8 PM (max offset 13 = 7 AM + 13 = 8 PM)
  // Bookings are 1 hour long, so 8 PM start means 9 PM end
  const hour = 7 + (hourOffset % 14); // Ensure within 7 AM - 8 PM range
  start.setHours(hour, 0, 0, 0);
  
  const end = new Date(start);
  end.setHours(start.getHours() + 1);

  return {
    startTime: start.toISOString().slice(0, 19),
    endTime: end.toISOString().slice(0, 19),
  };
}

/**
 * Generate full week time slot (Monday to Friday)
 * Time slots between 7 AM - 9 PM UTC+7 (Asia/Jakarta)
 * @param {number} weeksFromNow - Weeks from now
 * @param {number} hourOffset - Hour offset from 7 AM (0-13 for 7 AM to 8 PM)
 */
export function generateWeekdaySlots(weeksFromNow = 1, hourOffset = 0) {
  const slots = [];
  const start = new Date();
  
  // Move to the start of next week (Monday)
  const daysUntilMonday = (8 - start.getDay()) % 7 || 7;
  start.setDate(start.getDate() + daysUntilMonday + (weeksFromNow - 1) * 7);
  
  // Generate slots for Monday through Friday
  for (let i = 0; i < 5; i++) {
    const dayStart = new Date(start);
    dayStart.setDate(dayStart.getDate() + i);
    // Set hour between 7 AM and 8 PM (max offset 13 = 7 AM + 13 = 8 PM)
    dayStart.setHours(7 + (hourOffset % 14), 0, 0, 0);
    
    const dayEnd = new Date(dayStart);
    dayEnd.setHours(dayStart.getHours() + 1);
    
    slots.push({
      startTime: dayStart.toISOString().slice(0, 19),
      endTime: dayEnd.toISOString().slice(0, 19),
      dayName: ['Monday', 'Tuesday', 'Wednesday', 'Thursday', 'Friday'][i],
    });
  }
  
  return slots;
}

/**
 * Generate overlapping time slots (weekdays only)
 */
export function generateOverlappingSlots(daysFromNow = 1) {
  const start1 = new Date();
  start1.setDate(start1.getDate() + daysFromNow);
  
  // Skip weekends
  const day = start1.getDay();
  if (day === 6) {
    start1.setDate(start1.getDate() + 2);
  } else if (day === 0) {
    start1.setDate(start1.getDate() + 1);
  }
  
  start1.setHours(10, 0, 0, 0);
  
  const end1 = new Date(start1);
  end1.setHours(12, 0, 0, 0);

  const start2 = new Date(start1);
  start2.setHours(11, 0, 0, 0);
  
  const end2 = new Date(start2);
  end2.setHours(13, 0, 0, 0);

  return [
    {
      startTime: start1.toISOString().slice(0, 19),
      endTime: end1.toISOString().slice(0, 19),
    },
    {
      startTime: start2.toISOString().slice(0, 19),
      endTime: end2.toISOString().slice(0, 19),
    },
  ];
}

// Export for use in test scripts
export {
  BASE_URL,
  TOTAL_USERS,
  testUsers,
  tokenCache,
};
