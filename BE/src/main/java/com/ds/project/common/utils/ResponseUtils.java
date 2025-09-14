package com.ds.project.common.utils;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Utility class for creating standardized API responses
 */
public class ResponseUtils {
    
    /**
     * Create success response
     */
    public static <T> ResponseEntity<Map<String, Object>> success(T data) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("result", data);
        response.put("timestamp", LocalDateTime.now());
        return ResponseEntity.ok(response);
    }
    
    /**
     * Create success response with message
     */
    public static <T> ResponseEntity<Map<String, Object>> success(T data, String message) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", message);
        response.put("result", data);
        response.put("timestamp", LocalDateTime.now());
        return ResponseEntity.ok(response);
    }
    
    /**
     * Create error response
     */
    public static ResponseEntity<Map<String, Object>> error(String message, HttpStatus status) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", false);
        response.put("message", message);
        response.put("timestamp", LocalDateTime.now());
        return ResponseEntity.status(status).body(response);
    }
    
    /**
     * Create error response with details
     */
    public static ResponseEntity<Map<String, Object>> error(String message, String details, HttpStatus status) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", false);
        response.put("message", message);
        response.put("details", details);
        response.put("timestamp", LocalDateTime.now());
        return ResponseEntity.status(status).body(response);
    }
}
