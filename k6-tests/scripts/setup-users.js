import { testUsers, registerUser, login, tokenCache } from '../lib/api-client.js';
import { sleep } from 'k6';

// Configuration for setup
export const options = {
  setupTimeout: '15m',
  teardownTimeout: '5m',
};

// Setup: Create 300 users before tests run
export function setup() {
  console.log('🚀 Setting up 300 test users...');
  
  let created = 0;
  let existing = 0;
  let failed = 0;

  // Register users in batches
  for (let i = 0; i < testUsers.length; i++) {
    const user = testUsers[i];
    const success = registerUser(user);
    
    if (success) {
      created++;
    } else {
      existing++;
    }
    
    if ((i + 1) % 100 === 0) {
      console.log(`✅ Processed ${i + 1} users...`);
    }
    
    // Small delay to avoid overwhelming the server
    if (i % 50 === 0) {
      sleep(0.1);
    }
  }

  console.log(`✅ User setup complete: ${created} created, ${existing} existing`);
  
  // Login all users to cache tokens
  console.log('🔑 Logging in all users...');
  
  let loggedIn = 0;
  for (let i = 0; i < testUsers.length; i++) {
    const user = testUsers[i];
    const token = login(user.username, user.password);
    
    if (token) {
      loggedIn++;
    }
    
    if ((i + 1) % 100 === 0) {
      console.log(`🔑 Logged in ${i + 1} users...`);
    }
    
    // Small delay
    if (i % 50 === 0) {
      sleep(0.1);
    }
  }

  console.log(`✅ Login complete: ${loggedIn} users authenticated`);
  
  return {
    created,
    existing,
    loggedIn,
    timestamp: new Date().toISOString(),
  };
}

export default function() {
  // This is just a setup script, no test iterations
  console.log('Setup script completed. Use test scripts for actual testing.');
}

export function teardown(data) {
  console.log('📊 Setup Summary:');
  console.log(`   Created: ${data.created}`);
  console.log(`   Existing: ${data.existing}`);
  console.log(`   Logged In: ${data.loggedIn}`);
  console.log(`   Timestamp: ${data.timestamp}`);
}
