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
@RequestMapping("/api/v1/mixed")
@RequiredArgsConstructor
// Mặc định: yêu cầu phải đăng nhập
@AuthRequired
public class MixedSecurityController {

    /**
     * 🔓 PUBLIC ROUTE: Ghi đè @AuthRequired ở class
     * Sử dụng @PublicRoute để cho phép truy cập không cần token
     */
    @PublicRoute
    @GetMapping("/catalog")
    public ResponseEntity<Map<String, Object>> getCatalog() {
        Map<String, Object> response = new HashMap<>();
        response.put("message", "Product catalog - public access");
        response.put("products", java.util.Arrays.asList("Product 1", "Product 2", "Product 3"));
        response.put("timestamp", System.currentTimeMillis());
        response.put("access", "public");
        
        log.info("Public catalog accessed");
        return ResponseEntity.ok(response);
    }

    /**
     * 🔓 PUBLIC ROUTE: Search functionality
     */
    @PublicRoute
    @GetMapping("/search")
    public ResponseEntity<Map<String, Object>> searchProducts(@RequestParam String query) {
        Map<String, Object> response = new HashMap<>();
        response.put("message", "Search results - public access");
        response.put("query", query);
        response.put("results", java.util.Arrays.asList("Result 1", "Result 2"));
        response.put("timestamp", System.currentTimeMillis());
        response.put("access", "public");
        
        log.info("Public search executed with query: {}", query);
        return ResponseEntity.ok(response);
    }

    /**
     * 🔒 AUTHENTICATED ROUTE: Kế thừa @AuthRequired từ class
     * Không cần annotation gì thêm
     */
    @GetMapping("/profile")
    public ResponseEntity<Map<String, Object>> getUserProfile(@AuthenticationPrincipal Jwt jwt) {
        Map<String, Object> response = new HashMap<>();
        response.put("message", "User profile - authenticated access");
        response.put("userId", jwt.getSubject());
        response.put("email", jwt.getClaimAsString("email"));
        response.put("timestamp", System.currentTimeMillis());
        response.put("access", "authenticated");
        
        log.info("Authenticated profile accessed by user: {}", jwt.getSubject());
        return ResponseEntity.ok(response);
    }

    /**
     * 🔒 ROLE-BASED ROUTE: Yêu cầu role CUSTOMER
     */
    @PreAuthorize("hasRole('CUSTOMER')")
    @PostMapping("/orders")
    public ResponseEntity<Map<String, Object>> createOrder(
            @AuthenticationPrincipal Jwt jwt,
            @RequestBody Map<String, Object> orderRequest) {
        
        Map<String, Object> response = new HashMap<>();
        response.put("message", "Order created successfully");
        response.put("userId", jwt.getSubject());
        response.put("orderId", "ORD-" + System.currentTimeMillis());
        response.put("items", orderRequest.get("items"));
        response.put("timestamp", System.currentTimeMillis());
        response.put("requiredRole", "CUSTOMER");
        
        log.info("Order created by customer: {}", jwt.getSubject());
        return ResponseEntity.ok(response);
    }

    /**
     * 🔒 ROLE-BASED ROUTE: Yêu cầu role STAFF
     */
    @PreAuthorize("hasRole('STAFF')")
    @PutMapping("/orders/{orderId}/status")
    public ResponseEntity<Map<String, Object>> updateOrderStatus(
            @PathVariable String orderId,
            @RequestBody Map<String, Object> statusRequest) {
        
        Map<String, Object> response = new HashMap<>();
        response.put("message", "Order status updated");
        response.put("orderId", orderId);
        response.put("newStatus", statusRequest.get("status"));
        response.put("timestamp", System.currentTimeMillis());
        response.put("requiredRole", "STAFF");
        
        log.info("Order status updated: {} -> {}", orderId, statusRequest.get("status"));
        return ResponseEntity.ok(response);
    }

    /**
     * 🔒 SCOPE-BASED ROUTE: Yêu cầu scope cụ thể
     */
    @PreAuthorize("hasAuthority('SCOPE_orders:read')")
    @GetMapping("/orders/{orderId}")
    public ResponseEntity<Map<String, Object>> getOrderDetails(@PathVariable String orderId) {
        Map<String, Object> response = new HashMap<>();
        response.put("message", "Order details retrieved");
        response.put("orderId", orderId);
        response.put("orderDetails", Map.of(
            "customer", "Customer Name",
            "items", java.util.Arrays.asList("Item 1", "Item 2"),
            "total", 299.99
        ));
        response.put("timestamp", System.currentTimeMillis());
        response.put("requiredScope", "orders:read");
        
        log.info("Order details retrieved: {}", orderId);
        return ResponseEntity.ok(response);
    }

    /**
     * 🔒 CUSTOM EXPRESSION: Logic phức tạp
     * Chỉ cho phép user xem order của chính mình hoặc có role STAFF
     */
    @PreAuthorize("hasRole('STAFF') or authentication.principal.subject == #customerId")
    @GetMapping("/orders/customer/{customerId}")
    public ResponseEntity<Map<String, Object>> getCustomerOrders(
            @PathVariable String customerId,
            @AuthenticationPrincipal Jwt jwt) {
        
        Map<String, Object> response = new HashMap<>();
        response.put("message", "Customer orders retrieved");
        response.put("customerId", customerId);
        response.put("requestedBy", jwt.getSubject());
        response.put("orders", java.util.Arrays.asList("Order 1", "Order 2", "Order 3"));
        response.put("timestamp", System.currentTimeMillis());
        response.put("security", "self-or-staff");
        
        log.info("Customer orders retrieved for {} by {}", customerId, jwt.getSubject());
        return ResponseEntity.ok(response);
    }
}
