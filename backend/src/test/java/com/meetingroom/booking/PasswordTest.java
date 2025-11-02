package com.meetingroom.booking;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public class PasswordTest {
    
    @Test
    public void testPasswordMatch() {
        String storedHash = "$2a$12$MNKVblbm6/nMFWBPrcEOne62iULg34h8Q3AZtbjSBUf/s0DGiWsT2";
        String plainPassword = "password123";
        
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder(12);
        boolean matches = encoder.matches(plainPassword, storedHash);
        
        System.out.println("============================================");
        System.out.println("Password Test Results:");
        System.out.println("Plain password: " + plainPassword);
        System.out.println("Stored hash: " + storedHash);
        System.out.println("Match result: " + matches);
        System.out.println("============================================");
        
        if (matches) {
            System.out.println("✅ SUCCESS: Password 'password123' matches the hash!");
        } else {
            System.out.println("❌ FAILED: Password 'password123' does NOT match the hash!");
            System.out.println("\nThis means the database has a different password.");
            System.out.println("You need to either:");
            System.out.println("1. Reset the database (drop and recreate)");
            System.out.println("2. Update the user password in the database");
        }
    }
}
