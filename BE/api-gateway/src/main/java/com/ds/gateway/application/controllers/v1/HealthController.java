package com.ds.gateway.application.controllers.v1;

import com.ds.gateway.common.entities.dto.common.BaseResponse;
import com.ds.gateway.annotations.PublicRoute;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * Health check controller
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/health")
@RequiredArgsConstructor
public class HealthController {
    
    @Autowired
    @Qualifier("userServiceWebClient")
    private WebClient userServiceWebClient;
    
    @Autowired
    @Qualifier("settingsServiceWebClient")
    private WebClient settingsServiceWebClient;
    
    @Autowired
    @Qualifier("zoneServiceWebClient")
    private WebClient zoneServiceWebClient;
    
    @Autowired
    @Qualifier("parcelServiceWebClient")
    private WebClient parcelServiceWebClient;
    
    @Autowired
    @Qualifier("sessionServiceWebClient")
    private WebClient sessionServiceWebClient;
    
    @Autowired
    @Qualifier("communicationServiceWebClient")
    private WebClient communicationServiceWebClient;
    
    @PublicRoute
    @GetMapping
    public ResponseEntity<BaseResponse<Map<String, Object>>> health() {
        Map<String, Object> healthInfo = new HashMap<>();
        healthInfo.put("status", "UP");
        healthInfo.put("service", "api-gateway");
        healthInfo.put("timestamp", LocalDateTime.now());
        
        return ResponseEntity.ok(BaseResponse.success(healthInfo, "API Gateway is running"));
    }
    
    /**
     * Get health status of all services
     * GET /api/v1/health/all
     */
    @PublicRoute
    @GetMapping("/all")
    public ResponseEntity<BaseResponse<Map<String, Object>>> getAllServicesHealth() {
        log.debug("[api-gateway] [HealthController.getAllServicesHealth] GET /api/v1/health/all");
        
        Map<String, Object> allHealth = new HashMap<>();
        allHealth.put("api-gateway", Map.of("status", "UP", "timestamp", LocalDateTime.now()));
        
        // Check all services in parallel
        Map<String, CompletableFuture<Map<String, Object>>> futures = new HashMap<>();
        
        futures.put("user-service", checkServiceHealth(userServiceWebClient, "/actuator/health"));
        futures.put("settings-service", checkServiceHealth(settingsServiceWebClient, "/actuator/health"));
        futures.put("zone-service", checkServiceHealth(zoneServiceWebClient, "/health"));
        futures.put("parcel-service", checkServiceHealth(parcelServiceWebClient, "/actuator/health"));
        futures.put("session-service", checkServiceHealth(sessionServiceWebClient, "/actuator/health"));
        futures.put("communication-service", checkServiceHealth(communicationServiceWebClient, "/actuator/health"));
        
        // Wait for all checks to complete (with timeout)
        futures.forEach((serviceName, future) -> {
            try {
                Map<String, Object> health = future.get(5, TimeUnit.SECONDS);
                allHealth.put(serviceName, health);
            } catch (Exception e) {
                log.warn("Failed to check health for {}: {}", serviceName, e.getMessage());
                allHealth.put(serviceName, Map.of(
                    "status", "DOWN",
                    "error", e.getMessage(),
                    "timestamp", LocalDateTime.now()
                ));
            }
        });
        
        // Calculate overall status
        long upCount = allHealth.values().stream()
            .filter(health -> health instanceof Map && "UP".equals(((Map<?, ?>) health).get("status")))
            .count();
        
        String overallStatus = upCount == allHealth.size() ? "UP" : "DEGRADED";
        if (upCount == 0) {
            overallStatus = "DOWN";
        }
        
        Map<String, Object> result = new HashMap<>();
        result.put("overallStatus", overallStatus);
        result.put("services", allHealth);
        result.put("healthyCount", upCount);
        result.put("totalCount", allHealth.size());
        result.put("timestamp", LocalDateTime.now());
        
        return ResponseEntity.ok(BaseResponse.success(result, "All services health check"));
    }
    
    private CompletableFuture<Map<String, Object>> checkServiceHealth(WebClient webClient, String healthPath) {
        return webClient.get()
            .uri(healthPath)
            .retrieve()
            .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
            .timeout(java.time.Duration.ofSeconds(5))
            .onErrorResume(ex -> {
                log.debug("Health check failed for {}: {}", healthPath, ex.getMessage());
                Map<String, Object> errorHealth = new HashMap<>();
                errorHealth.put("status", "DOWN");
                errorHealth.put("error", ex.getMessage());
                errorHealth.put("timestamp", LocalDateTime.now());
                return Mono.just(errorHealth);
            })
            .toFuture();
    }
}
