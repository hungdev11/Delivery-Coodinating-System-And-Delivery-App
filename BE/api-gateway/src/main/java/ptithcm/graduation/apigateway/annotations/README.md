# Security Annotations Guide

## Tổng quan

Hệ thống sử dụng **annotation-based security** để quản lý quyền truy cập API. Thay vì cấu hình security ở `SecurityConfig`, bạn có thể đánh dấu trực tiếp trên controller và method.

## Các Annotation có sẵn

### 1. `@AuthRequired`
- **Mục đích**: Đánh dấu class/method yêu cầu authentication
- **Sử dụng**: Có thể dùng ở class level hoặc method level
- **Logic**: Tương đương với `@PreAuthorize("isAuthenticated()")`

### 2. `@PublicRoute`
- **Mục đích**: Đánh dấu method là public, không cần authentication
- **Sử dụng**: Chỉ dùng ở method level
- **Logic**: Ghi đè `@AuthRequired` ở class level

## Cách sử dụng

### Trường hợp 1: Controller hoàn toàn public
```java
@RestController
@RequestMapping("/api/v1/public")
public class PublicController {
    // Tất cả methods đều public, không cần annotation gì
    @GetMapping("/health")
    public ResponseEntity<String> health() { ... }
}
```

### Trường hợp 2: Controller mặc định yêu cầu auth
```java
@RestController
@RequestMapping("/api/v1/users")
@AuthRequired  // Mặc định yêu cầu đăng nhập
public class UserController {
    
    @PublicRoute  // Ghi đè: method này public
    @GetMapping("/public-info")
    public ResponseEntity<String> publicInfo() { ... }
    
    // Method này yêu cầu auth (kế thừa từ class)
    @GetMapping("/me")
    public ResponseEntity<String> me() { ... }
    
    // Method này yêu cầu role ADMIN
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/create")
    public ResponseEntity<String> create() { ... }
}
```

### Trường hợp 3: Mixed security trong cùng controller
```java
@RestController
@RequestMapping("/api/v1/mixed")
@AuthRequired  // Mặc định yêu cầu auth
public class MixedController {
    
    @PublicRoute  // Public
    @GetMapping("/catalog")
    public ResponseEntity<String> catalog() { ... }
    
    // Authenticated (kế thừa từ class)
    @GetMapping("/profile")
    public ResponseEntity<String> profile() { ... }
    
    // Role-based
    @PreAuthorize("hasRole('CUSTOMER')")
    @PostMapping("/orders")
    public ResponseEntity<String> createOrder() { ... }
}
```

## Các loại security khác

### Role-based Security
```java
@PreAuthorize("hasRole('ADMIN')")
@PostMapping("/admin-action")
public ResponseEntity<String> adminAction() { ... }

@PreAuthorize("hasRole('MANAGER')")
@PutMapping("/manager-action")
public ResponseEntity<String> managerAction() { ... }
```

### Scope-based Security
```java
@PreAuthorize("hasAuthority('SCOPE_users:write')")
@PutMapping("/{id}")
public ResponseEntity<String> updateUser() { ... }
```

### Custom Expression
```java
// Chỉ cho phép user sửa thông tin của chính mình
@PreAuthorize("authentication.principal.subject == #userId")
@PatchMapping("/profile/{userId}")
public ResponseEntity<String> updateOwnProfile(@PathVariable String userId) { ... }

// Logic phức tạp hơn
@PreAuthorize("hasRole('STAFF') or authentication.principal.subject == #customerId")
@GetMapping("/orders/customer/{customerId}")
public ResponseEntity<String> getCustomerOrders(@PathVariable String customerId) { ... }
```

## Lưu ý quan trọng

### 1. Thứ tự ưu tiên
- `@PublicRoute` ở method level **ghi đè** `@AuthRequired` ở class level
- `@PreAuthorize` ở method level **ghi đè** tất cả annotation khác

### 2. Mặc định an toàn
- Nếu không có annotation gì, method sẽ **public** (vì `permitAll()` ở SecurityConfig)
- **Luôn** sử dụng `@AuthRequired` ở class level để đảm bảo an toàn

### 3. JWT Claims
- `jwt.getSubject()`: User ID
- `jwt.getClaimAsString("email")`: Email
- `jwt.getClaimAsStringList("roles")`: Danh sách roles
- `jwt.getClaimAsString("preferred_username")`: Username

### 4. Error Handling
- **401 Unauthorized**: Không có token hoặc token không hợp lệ
- **403 Forbidden**: Có token nhưng thiếu quyền (role/scope)

## Ví dụ thực tế

Xem các controller mẫu:
- `DemoController`: Demo đầy đủ các trường hợp
- `PublicController`: Controller hoàn toàn public
- `MixedSecurityController`: Controller với mixed security

## Testing

### Test public routes
```bash
curl http://localhost:8080/api/v1/public/health
curl http://localhost:8080/api/v1/demo/public-info
```

### Test authenticated routes
```bash
curl -H "Authorization: Bearer YOUR_JWT_TOKEN" \
     http://localhost:8080/api/v1/demo/me
```

### Test role-based routes
```bash
curl -H "Authorization: Bearer ADMIN_JWT_TOKEN" \
     -X POST http://localhost:8080/api/v1/demo/admin-action \
     -H "Content-Type: application/json" \
     -d '{"action": "test"}'
```
