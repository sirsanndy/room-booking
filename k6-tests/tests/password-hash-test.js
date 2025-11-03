import crypto from 'k6/crypto';

/**
 * Test to verify SHA-256 password hashing matches frontend implementation
 */
export default function() {
  const password = "password123";
  const expectedHash = "ef92b778bafe771e89245b89ecbc08a44a4e166c06659911881f383d4473e94f";
  
  const actualHash = crypto.sha256(password, 'hex');
  
  console.log('='.repeat(60));
  console.log('SHA-256 Password Hashing Test');
  console.log('='.repeat(60));
  console.log(`Password:      ${password}`);
  console.log(`Expected Hash: ${expectedHash}`);
  console.log(`Actual Hash:   ${actualHash}`);
  console.log(`Match:         ${actualHash === expectedHash ? '✅ YES' : '❌ NO'}`);
  console.log('='.repeat(60));
  
  if (actualHash !== expectedHash) {
    throw new Error('SHA-256 hash mismatch! K6 hashing does not match frontend.');
  }
  
  console.log('✅ K6 SHA-256 hashing matches frontend implementation!');
}
