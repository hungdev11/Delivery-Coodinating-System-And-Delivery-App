package com.ds.communication_service.application.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Controller for seed progress session management
 */
@RestController
@RequestMapping("/api/v1/seed-progress")
@RequiredArgsConstructor
@Slf4j
public class SeedProgressController {
    
    /**
     * Generate a new session key for seed progress tracking
     * Client will use this key to subscribe to WebSocket topic: /topic/seed-progress/{sessionKey}
     * 
     * @return Session key for seed progress tracking
     */
    @PostMapping("/session-key")
    public ResponseEntity<Map<String, Object>> generateSessionKey() {
        log.info("[communication-service] [SeedProgressController.generateSessionKey] Generating new session key for seed progress");
        
        // Generate unique session key
        String sessionKey = "seed-" + UUID.randomUUID().toString();
        
        Map<String, Object> response = new HashMap<>();
        response.put("sessionKey", sessionKey);
        response.put("websocketTopic", "/topic/seed-progress/" + sessionKey);
        
        log.debug("[communication-service] [SeedProgressController.generateSessionKey] Generated session key: {}", sessionKey);
        
        return ResponseEntity.ok(response);
    }
}
