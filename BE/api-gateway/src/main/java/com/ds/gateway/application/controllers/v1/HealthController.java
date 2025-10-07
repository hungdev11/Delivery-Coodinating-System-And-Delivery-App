package com.ds.gateway.application.controllers.v1;

import com.ds.gateway.common.entities.dto.common.BaseResponse;
import com.ds.gateway.annotations.PublicRoute;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Health check controller
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/health")
public class HealthController {
    
    @PublicRoute
    @GetMapping
    public ResponseEntity<BaseResponse<Map<String, Object>>> health() {
        Map<String, Object> healthInfo = new HashMap<>();
        healthInfo.put("status", "UP");
        healthInfo.put("service", "api-gateway");
        healthInfo.put("timestamp", LocalDateTime.now());
        
        return ResponseEntity.ok(BaseResponse.success(healthInfo, "API Gateway is running"));
    }
}
