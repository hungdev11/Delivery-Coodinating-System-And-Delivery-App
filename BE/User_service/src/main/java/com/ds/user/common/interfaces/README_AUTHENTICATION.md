# Authentication & Identity Provider Abstraction

## Overview
Kiến trúc này tách biệt business logic khỏi implementation cụ thể của identity provider (Keycloak, Auth0, AWS Cognito, etc.)

## Architecture

```
┌─────────────────────────────────────────┐
│   Business Layer (UserService)          │
│   - Depends on Interfaces only          │
└─────────────┬───────────────────────────┘
              │
              │ depends on
              ▼
┌─────────────────────────────────────────┐
│   Interfaces (common/interfaces)        │
│   - IAuthenticationService              │
│   - IIdentityProvider                   │
└─────────────┬───────────────────────────┘
              │
              │ implements
              ▼
┌─────────────────────────────────────────┐
│   Implementations (business/services)   │
│   - KeycloakAuthenticationService       │
│   - KeycloakIdentityProvider            │
└─────────────────────────────────────────┘
```

## Interfaces

### 1. IAuthenticationService
**Mục đích**: Xử lý authentication (login, logout, token management)

**Methods**:
- `AuthTokenDto login(username, password)` - Đăng nhập
- `AuthTokenDto refreshToken(refreshToken)` - Refresh token
- `void logout(refreshToken)` - Đăng xuất
- `boolean verifyToken(accessToken)` - Verify token

### 2. IIdentityProvider
**Mục đích**: Quản lý user trên identity provider

**Methods**:
- `String createUser(...)` - Tạo user
- `ExternalUserDto getUserById(externalId)` - Lấy thông tin user
- `void updateUser(...)` - Cập nhật user
- `void deleteUser(externalId)` - Xóa user
- `void updatePassword(...)` - Đổi password
- `List<String> getUserRoles(externalId)` - Lấy roles
- `void assignRoles(...)` - Gán roles
- `void sendEmailVerification(...)` - Gửi email verification
- `void sendPasswordResetEmail(...)` - Gửi email reset password

## DTOs

### AuthTokenDto
```java
{
  accessToken: String,
  refreshToken: String,
  tokenType: String,
  expiresIn: Integer,
  refreshExpiresIn: Integer
}
```

### ExternalUserDto
```java
{
  id: String,
  username: String,
  email: String,
  emailVerified: Boolean,
  enabled: Boolean,
  firstName: String,
  lastName: String,
  roles: List<String>,
  attributes: Map<String, Object>,
  createdTimestamp: Long
}
```

## How to Replace Keycloak

### Step 1: Create New Implementation
Tạo class mới implement `IAuthenticationService` và `IIdentityProvider`:

```java
@Service
@Primary  // Sử dụng @Primary để override Keycloak implementation
public class Auth0AuthenticationService implements IAuthenticationService {
    // Implement all methods using Auth0 SDK
}

@Service
@Primary
public class Auth0IdentityProvider implements IIdentityProvider {
    // Implement all methods using Auth0 SDK
}
```

### Step 2: Update Dependencies
```xml
<!-- Remove Keycloak dependencies -->
<!-- Add new provider dependencies (Auth0, Cognito, etc.) -->
```

### Step 3: Update Configuration
```yaml
# Remove keycloak config
# Add new provider config
auth0:
  domain: ${AUTH0_DOMAIN}
  client-id: ${AUTH0_CLIENT_ID}
  client-secret: ${AUTH0_CLIENT_SECRET}
```

### Step 4: Remove Keycloak Implementation
- Xóa `KeycloakAuthenticationService`
- Xóa `KeycloakIdentityProvider`
- Xóa `KeycloakAdminConfig`

### Step 5: Test
- Business logic trong `UserService` KHÔNG CẦN THAY ĐỔI
- Chỉ cần test integration với provider mới

## Benefits

✅ **Loose Coupling**: Business logic không phụ thuộc vào Keycloak  
✅ **Easy Testing**: Mock interfaces thay vì mock Keycloak client  
✅ **Easy Migration**: Chỉ cần implement interfaces mới  
✅ **Multiple Providers**: Có thể support nhiều provider cùng lúc  
✅ **Clear Contract**: Interface định nghĩa rõ ràng operations cần thiết  

## Example Usage in Business Layer

```java
@Service
public class UserService implements IUserService {
    
    @Autowired
    private IAuthenticationService authService;  // Interface, not concrete class
    
    @Autowired
    private IIdentityProvider identityProvider;  // Interface, not concrete class
    
    public LoginResponse login(String username, String password) {
        // Use interface methods
        AuthTokenDto tokens = authService.login(username, password);
        
        // Business logic here...
        
        return buildLoginResponse(tokens);
    }
}
```

## Notes

- **NEVER** import Keycloak classes trong business layer
- **ALWAYS** sử dụng interfaces (`IAuthenticationService`, `IIdentityProvider`)
- **DTOs** phải generic (không chứa Keycloak-specific fields)
- **Exceptions** nên wrap lại thành business exceptions
