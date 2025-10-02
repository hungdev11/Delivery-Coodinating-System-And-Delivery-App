# Keycloak Migration Guide

## Overview
This document outlines the migration from User Service-based authentication to Keycloak-based authentication in the API Gateway.

## Changes Made

### 1. New Keycloak Service (`KeycloakAuthService`)
- **File**: `src/main/java/com/ds/gateway/business/v1/services/KeycloakAuthService.java`
- **Purpose**: Handles JWT token validation, user info extraction, and token refresh with Keycloak
- **Key Methods**:
  - `validateTokenAndGetUserInfo()` - Validate JWT and get user info from Keycloak
  - `refreshToken()` - Refresh access token using refresh token
  - `extractUserInfoFromJwt()` - Extract user info from validated JWT
  - `isTokenValid()` - Check if token is still valid
  - `logout()` - Logout user by invalidating refresh token

### 2. New DTOs for Keycloak
- **KeycloakTokenRequestDto** - For token requests
- **KeycloakTokenResponseDto** - For token responses
- **KeycloakUserInfoDto** - For user information from JWT

### 3. Updated AuthController
- **File**: `src/main/java/com/ds/gateway/application/controllers/v1/AuthController.java`
- **Removed Endpoints**:
  - `/login` - Now handled directly by Keycloak
  - `/register` - Now handled directly by Keycloak
- **Updated Endpoints**:
  - `/refresh-token` - Now uses Keycloak service
  - `/logout` - Now uses Keycloak service
  - `/sync` - Enhanced to use Keycloak user info extraction
- **New Endpoints**:
  - `/validate-token` - Validate JWT token and return user info
  - `/me` - Get current user info from JWT

### 4. WebClient Configuration
- **File**: `src/main/java/com/ds/gateway/application/configs/WebClientConfig.java`
- **Added**: `keycloakWebClient` bean for Keycloak API calls

## New Authentication Flow

### 1. Login Flow
```
Client → Keycloak (direct) → JWT Token
Client → API Gateway /validate-token → User Info
Client → API Gateway /sync → Sync user data to User Service
```

### 2. API Access Flow
```
Client → API Gateway (with JWT) → Validate JWT → Business Logic
```

### 3. Token Refresh Flow
```
Client → API Gateway /refresh-token → Keycloak → New tokens
```

## Configuration

### Environment Variables
```bash
KEYCLOAK_URL=http://localhost:8080
KEYCLOAK_REALM=keycloak
KEYCLOAK_CLIENT_REALM=delivery-system-client
```

### Application Configuration
The following Keycloak configuration is already present in `application.yml`:
```yaml
keycloak:
  auth-server-url: ${KEYCLOAK_URL:http://localhost:8080}
  backend:
    realm: ${KEYCLOAK_REALM:keycloak}
    issuer-uri: ${KEYCLOAK_URL:http://localhost:8080}/realms/${KEYCLOAK_REALM:keycloak}
    jwk-set-uri: ${KEYCLOAK_URL:http://localhost:8080}/realms/${KEYCLOAK_REALM:keycloak}/protocol/openid-connect/certs
  client:
    realm: ${KEYCLOAK_CLIENT_REALM:delivery-system-client}
    issuer-uri: ${KEYCLOAK_URL:http://localhost:8080}/realms/${KEYCLOAK_CLIENT_REALM:delivery-system-client}
    jwk-set-uri: ${KEYCLOAK_URL:http://localhost:8080}/realms/${KEYCLOAK_CLIENT_REALM:delivery-system-client}/protocol/openid-connect/certs
```

## API Endpoints

### Public Endpoints (No Authentication Required)
- `POST /api/v1/auth/validate-token` - Validate JWT token
- `POST /api/v1/auth/refresh-token` - Refresh access token
- `POST /api/v1/auth/logout` - Logout user

### Protected Endpoints (Authentication Required)
- `GET /api/v1/auth/me` - Get current user info
- `POST /api/v1/auth/sync` - Sync user data to User Service

## Migration Benefits

1. **Security**: Keycloak provides enterprise-grade authentication
2. **Scalability**: Authentication is decoupled from User Service
3. **Standards**: Uses OAuth2/OpenID Connect standards
4. **Multi-realm**: Supports multiple realms (backend and client)
5. **Token Management**: Better token lifecycle management

## Deprecated Components

### AuthServiceClient (Deprecated)
- **File**: `src/main/java/com/ds/gateway/business/v1/services/AuthServiceClient.java`
- **Status**: Deprecated - replaced by KeycloakAuthService
- **Note**: Can be removed after full migration validation

## Testing

### Manual Testing Steps
1. **Login via Keycloak**: Use Keycloak admin console or direct API calls
2. **Validate Token**: Call `/api/v1/auth/validate-token` with JWT
3. **Get User Info**: Call `/api/v1/auth/me` with JWT
4. **Sync User**: Call `/api/v1/auth/sync` with JWT
5. **Refresh Token**: Call `/api/v1/auth/refresh-token` with refresh token
6. **Logout**: Call `/api/v1/auth/logout` with refresh token

### Integration Testing
- Test with both backend and client realms
- Test token expiration and refresh
- Test user sync functionality
- Test error handling for invalid tokens

## Next Steps

1. **Remove AuthServiceClient**: After validation, remove the deprecated service
2. **Update Client Applications**: Update frontend to use Keycloak login
3. **Update Documentation**: Update API documentation
4. **Monitor**: Monitor authentication flow in production
