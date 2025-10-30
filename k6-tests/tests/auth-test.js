import { check } from 'k6';
import { login, registerUser } from '../lib/api-client.js';

// Auth test with concurrent users
export const options = {
  vus: 5,
  iterations: 5,
  setupTimeout: '15m',
  teardownTimeout: '5m',
};

export function setup() {
  console.log('🧪 Testing Authentication with 5 concurrent users...\n');
  
  // Create 5 test users
  const testUsers = [];
  for (let i = 1; i <= 5; i++) {
    const user = {
      username: `k6testuser${i}`,
      password: 'Test@1234',
      email: `k6testuser${i}@test.com`,
      fullName: `K6 Test User ${i}`,
    };
    
    console.log(`📝 Registering user ${i}: ${user.username}`);
    registerUser(user);
    testUsers.push(user);
  }
  
  return { users: testUsers };
}

export default function (data) {
  const userIndex = (__VU - 1) % data.users.length;
  const testUser = data.users[userIndex];
  
  console.log(`\n🔐 VU ${__VU}: Attempting login...`);
  console.log(`   Username: ${testUser.username}`);
  console.log(`   Password: ${testUser.password}`);
  
  const token = login(testUser.username, testUser.password);
  
  const authSuccess = check(token, {
    'Token obtained': (t) => t !== null && t !== undefined,
    'Token is string': (t) => typeof t === 'string',
    'Token is not empty': (t) => t && t.length > 0,
    'Token looks like JWT': (t) => t && t.split('.').length === 3,
  });
  
  if (authSuccess) {
    console.log(`\n✅ VU ${__VU}: Authentication SUCCESS!`);
    console.log(`   User: ${testUser.username}`);
    console.log(`   Token: ${token.substring(0, 50)}...`);
    console.log(`   Token length: ${token.length} characters`);
    console.log(`   Token parts: ${token.split('.').length} (should be 3 for JWT)`);
  } else {
    console.error(`\n❌ VU ${__VU}: Authentication FAILED!`);
    console.error(`   User: ${testUser.username}`);
    console.error(`   Token value: ${token}`);
  }
}

export function teardown(data) {
  console.log('\n📊 Test Complete');
  console.log(`   Total users tested: ${data.users.length}`);
  console.log(`   Users: ${data.users.map(u => u.username).join(', ')}`);
}
