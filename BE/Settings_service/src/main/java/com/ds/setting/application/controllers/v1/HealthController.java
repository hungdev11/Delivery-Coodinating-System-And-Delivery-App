package com.ds.setting.application.controllers.v1;

import com.ds.setting.common.entities.dto.common.BaseResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Health Check Controller for Settings Service
 */
@Slf4j
@RestController
@RequestMapping
@RequiredArgsConstructor
@Tag(name = "Health", description = "Health check endpoints")
public class HealthController {

    @GetMapping("/health")
    @Operation(summary = "Check service health")
    public ResponseEntity<BaseResponse<Map<String, Object>>> health() {
        Map<String, Object> healthInfo = new HashMap<>();
        healthInfo.put("status", "UP");
        healthInfo.put("service", "settings-service");
        healthInfo.put("timestamp", LocalDateTime.now());

        return ResponseEntity.ok(BaseResponse.success(healthInfo, "Settings Service is running"));
    }

    @GetMapping("/actuator/health")
    @Operation(summary = "Actuator health endpoint")
    public ResponseEntity<Map<String, Object>> actuatorHealth() {
        Map<String, Object> health = new HashMap<>();
        health.put("status", "UP");
        
        Map<String, Object> details = new HashMap<>();
        details.put("service", "settings-service");
        details.put("timestamp", LocalDateTime.now());
        
        health.put("details", details);
        
        return ResponseEntity.ok(health);
    }
}
