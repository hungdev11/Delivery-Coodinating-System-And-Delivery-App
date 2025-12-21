package com.ds.gateway.application.controllers.v1;

import com.ds.gateway.common.entities.dto.common.BaseResponse;
import com.ds.gateway.common.entities.dto.health.HealthStatusDto;
import com.ds.gateway.infrastructure.health.HealthStatusTracker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * REST API for health monitoring
 * Provides health status information for all services
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/health-monitoring")
@RequiredArgsConstructor
public class HealthMonitoringController {
    
    private final HealthStatusTracker healthStatusTracker;
    
    /**
     * Get current health status of all services
     */
    @GetMapping("/status")
    public BaseResponse<Map<String, Object>> getHealthStatus() {
        Map<String, Object> result = new HashMap<>();
        Map<String, HealthStatusDto> allStatuses = healthStatusTracker.getAllStatuses();
        
        Map<String, Object> services = new HashMap<>();
        for (Map.Entry<String, HealthStatusDto> entry : allStatuses.entrySet()) {
            String serviceName = entry.getKey();
            HealthStatusDto status = entry.getValue();
            boolean isDown = healthStatusTracker.isServiceDown(serviceName);
            
            Map<String, Object> serviceInfo = new HashMap<>();
            serviceInfo.put("status", isDown ? "DOWN" : status.getStatus());
            serviceInfo.put("lastUpdate", status.getTimestamp());
            serviceInfo.put("version", status.getVersion() != null ? status.getVersion() : "unknown");
            serviceInfo.put("isDown", isDown);
            serviceInfo.put("metadata", status.getMetadata());
            
            services.put(serviceName, serviceInfo);
        }
        
        result.put("services", services);
        result.put("totalServices", services.size());
        result.put("timestamp", java.time.LocalDateTime.now());
        
        return BaseResponse.success(result);
    }
    
    /**
     * Get health status of a specific service
     */
    @GetMapping("/status/{serviceName}")
    public BaseResponse<Map<String, Object>> getServiceHealth(@PathVariable String serviceName) {
        HealthStatusDto status = healthStatusTracker.getLatestStatus(serviceName);
        
        if (status == null) {
            Map<String, Object> result = new HashMap<>();
            result.put("serviceName", serviceName);
            result.put("status", "UNKNOWN");
            result.put("isDown", true);
            result.put("message", "No health status received from this service");
            return BaseResponse.success(result);
        }
        
        boolean isDown = healthStatusTracker.isServiceDown(serviceName);
        
        Map<String, Object> result = new HashMap<>();
        result.put("serviceName", serviceName);
        result.put("status", isDown ? "DOWN" : status.getStatus());
        result.put("lastUpdate", status.getTimestamp());
        result.put("version", status.getVersion());
        result.put("isDown", isDown);
        result.put("metadata", status.getMetadata());
        
        return BaseResponse.success(result);
    }
}
