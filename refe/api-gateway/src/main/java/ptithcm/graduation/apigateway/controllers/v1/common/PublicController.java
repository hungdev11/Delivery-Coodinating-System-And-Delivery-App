package ptithcm.graduation.apigateway.controllers.v1.common;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ptithcm.graduation.apigateway.annotations.PublicRoute;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/v1/public")
@RequiredArgsConstructor
// Controller này hoàn toàn public, không cần @AuthRequired
public class PublicController {

    /**
     * 🔓 PUBLIC ROUTE: Không cần token
     * Không cần annotation gì thêm vì controller không có @AuthRequired
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "UP");
        response.put("service", "API Gateway");
        response.put("timestamp", System.currentTimeMillis());
        response.put("version", "1.0.0");
        
        log.info("Health check accessed");
        return ResponseEntity.ok(response);
    }

    /**
     * 🔓 PUBLIC ROUTE: Thông tin hệ thống
     */
    @GetMapping("/info")
    public ResponseEntity<Map<String, Object>> systemInfo() {
        Map<String, Object> response = new HashMap<>();
        response.put("message", "System information");
        response.put("environment", "development");
        response.put("javaVersion", System.getProperty("java.version"));
        response.put("timestamp", System.currentTimeMillis());
        
        log.info("System info accessed");
        return ResponseEntity.ok(response);
    }

    /**
     * 🔓 PUBLIC ROUTE: API documentation
     */
    @GetMapping("/docs")
    public ResponseEntity<Map<String, Object>> apiDocs() {
        Map<String, Object> response = new HashMap<>();
        response.put("message", "API Documentation");
        response.put("swaggerUrl", "/swagger-ui/index.html");
        response.put("openApiUrl", "/v3/api-docs");
        response.put("timestamp", System.currentTimeMillis());
        
        log.info("API docs accessed");
        return ResponseEntity.ok(response);
    }

    /**
     * 🔓 PUBLIC ROUTE: Contact information
     */
    @GetMapping("/contact")
    public ResponseEntity<Map<String, Object>> contact() {
        Map<String, Object> response = new HashMap<>();
        response.put("message", "Contact information");
        response.put("email", "support@example.com");
        response.put("phone", "+84-123-456-789");
        response.put("address", "Ho Chi Minh City, Vietnam");
        response.put("timestamp", System.currentTimeMillis());
        
        log.info("Contact info accessed");
        return ResponseEntity.ok(response);
    }
}
