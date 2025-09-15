package ptithcm.graduation.apigateway.controllers.v1.common;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import ptithcm.graduation.apigateway.annotations.AuthRequired;
import ptithcm.graduation.apigateway.annotations.PublicRoute;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/v1/demo")
@RequiredArgsConstructor
// Mặc định: yêu cầu phải đăng nhập (vào được đây thì mới xử lý)
@AuthRequired
public class DemoController {

    /**
     * 🔓 PUBLIC ROUTE: Không cần token
     * Sử dụng @PublicRoute annotation để ghi đè @AuthRequired ở class
     */
    @PublicRoute
    @GetMapping("/public-info")
    public ResponseEntity<Map<String, Object>> publicInfo() {
        Map<String, Object> response = new HashMap<>();
        response.put("message", "This is public information");
        response.put("timestamp", System.currentTimeMillis());
        response.put("status", "public");
        
        log.info("Public route accessed");
        return ResponseEntity.ok(response);
    }

    /**
     * 🔒 AUTHENTICATED ROUTE: Chỉ user đăng nhập (khớp với @AuthRequired ở class)
     * Không cần annotation gì thêm, tự động kế thừa từ class
     */
    @GetMapping("/me")
    public ResponseEntity<Map<String, Object>> me(@AuthenticationPrincipal Jwt jwt) {
        Map<String, Object> response = new HashMap<>();
        response.put("message", "User profile information");
        response.put("userId", jwt.getSubject());
        response.put("email", jwt.getClaimAsString("email"));
        response.put("roles", jwt.getClaimAsStringList("roles"));
        response.put("timestamp", System.currentTimeMillis());
        
        log.info("Authenticated route accessed by user: {}", jwt.getSubject());
        return ResponseEntity.ok(response);
    }

    /**
     * 🔒 ROLE-BASED ROUTE: Yêu cầu role ADMIN
     * Sử dụng @PreAuthorize để kiểm tra role cụ thể
     */
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/admin-action")
    public ResponseEntity<Map<String, Object>> adminAction(@RequestBody Map<String, Object> request) {
        Map<String, Object> response = new HashMap<>();
        response.put("message", "Admin action executed successfully");
        response.put("action", request.get("action"));
        response.put("timestamp", System.currentTimeMillis());
        response.put("requiredRole", "ADMIN");
        
        log.info("Admin action executed: {}", request.get("action"));
        return ResponseEntity.ok(response);
    }

    /**
     * 🔒 ROLE-BASED ROUTE: Yêu cầu role MANAGER
     * Sử dụng @PreAuthorize để kiểm tra role cụ thể
     */
    @PreAuthorize("hasRole('MANAGER')")
    @PutMapping("/manager-action/{id}")
    public ResponseEntity<Map<String, Object>> managerAction(
            @PathVariable String id,
            @RequestBody Map<String, Object> request) {
        
        Map<String, Object> response = new HashMap<>();
        response.put("message", "Manager action executed successfully");
        response.put("resourceId", id);
        response.put("action", request.get("action"));
        response.put("timestamp", System.currentTimeMillis());
        response.put("requiredRole", "MANAGER");
        
        log.info("Manager action executed on resource {}: {}", id, request.get("action"));
        return ResponseEntity.ok(response);
    }

    /**
     * 🔒 SCOPE-BASED ROUTE: Yêu cầu scope cụ thể (nếu dùng OAuth2 scopes)
     * Sử dụng @PreAuthorize để kiểm tra authority
     */
    @PreAuthorize("hasAuthority('SCOPE_demo:write')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> deleteResource(@PathVariable String id) {
        Map<String, Object> response = new HashMap<>();
        response.put("message", "Resource deleted successfully");
        response.put("resourceId", id);
        response.put("timestamp", System.currentTimeMillis());
        response.put("requiredScope", "demo:write");
        
        log.info("Resource deleted: {}", id);
        return ResponseEntity.ok(response);
    }

    /**
     * 🔒 CUSTOM EXPRESSION: Yêu cầu logic phức tạp hơn
     * Ví dụ: chỉ cho phép user sửa thông tin của chính mình
     */
    @PreAuthorize("authentication.principal.subject == #userId")
    @PatchMapping("/profile/{userId}")
    public ResponseEntity<Map<String, Object>> updateOwnProfile(
            @PathVariable String userId,
            @RequestBody Map<String, Object> request) {
        
        Map<String, Object> response = new HashMap<>();
        response.put("message", "Profile updated successfully");
        response.put("userId", userId);
        response.put("updatedFields", request.keySet());
        response.put("timestamp", System.currentTimeMillis());
        response.put("security", "self-only");
        
        log.info("Profile updated for user: {}", userId);
        return ResponseEntity.ok(response);
    }
}
