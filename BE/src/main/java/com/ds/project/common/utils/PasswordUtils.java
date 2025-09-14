package com.ds.project.common.utils;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Utility class for password operations
 */
public class PasswordUtils {
    
    private static final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    
    /**
     * Encode password using BCrypt
     */
    public static String encodePassword(String rawPassword) {
        return passwordEncoder.encode(rawPassword);
    }
    
    /**
     * Verify password against encoded password
     */
    public static boolean verifyPassword(String rawPassword, String encodedPassword) {
        return passwordEncoder.matches(rawPassword, encodedPassword);
    }
    
    /**
     * Get password encoder instance
     */
    public static PasswordEncoder getPasswordEncoder() {
        return passwordEncoder;
    }

    public static boolean matches(String password, String password2) {
        return passwordEncoder.matches(password, password2);
    }
}
