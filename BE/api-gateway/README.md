# API Gateway

## Overview

The API Gateway serves as the single entry point for all client requests in the Delivery System. It handles authentication, authorization, request routing, and proxying to backend microservices.

## Architecture

### Technology Stack

- **Framework**: Spring Boot 3.x
- **Language**: Java 17+
- **Authentication**: Keycloak (OAuth 2.0 / OpenID Connect)
- **HTTP Client**: WebClient (Spring WebFlux)
- **Security**: Spring Security with JWT validation
- **Build Tool**: Maven

### Key Responsibilities

- Authentication & Authorization (via Keycloak)
- Request routing and proxying to microservices
- JWT token validation and user context management
- CORS configuration
- Centralized error handling
- API documentation (OpenAPI/Swagger)

## Port Configuration

- **Development**: `21500`
- **Base URL**: `http://localhost:21500/api/v1`

## Database

The API Gateway does not have its own database. It relies on:
- Keycloak for authentication
- Backend services for data operations

## Environment Configuration

### Environment Variables (from root `env.local`)

```bash
# Server Configuration
API_GATEWAY_PORT=21500

# Keycloak Configuration
KEYCLOAK_HOST=localhost
KEYCLOAK_PORT=8080
KEYCLOAK_REALM_BACKEND=delivery-system-backend
KEYCLOAK_REALM_CLIENT=delivery-system-client
KEYCLOAK_CLIENT_BACKEND_ID=delivery-backend
KEYCLOAK_CLIENT_WEB_ID=delivery-management-web
KEYCLOAK_CLIENT_MOBILE_ID=delivery-mobile-app

# Backend Service URLs
USER_SERVICE_URL=http://localhost:21501
SETTINGS_SERVICE_URL=http://localhost:21502
ZONE_SERVICE_URL=http://localhost:21503

# CORS Configuration
CORS_ALLOWED_ORIGINS=http://localhost:3000,http://localhost:5173,http://localhost:8080
```

### Application Settings (`application.yaml`)

```yaml
server:
  port: 21500

spring:
  application:
    name: api-gateway

keycloak:
  host: ${KEYCLOAK_HOST}
  port: ${KEYCLOAK_PORT}
  realm-backend: ${KEYCLOAK_REALM_BACKEND}
  realm-client: ${KEYCLOAK_REALM_CLIENT}
```

## Features

### 1. Authentication & Authorization

- Keycloak integration for OAuth 2.0 / OpenID Connect
- JWT token validation on protected endpoints
- Role-based access control (RBAC)
- Public route annotations (`@PublicRoute`)

### 2. Service Proxying

The gateway proxies requests to backend services:

| Route Prefix | Target Service | Port | Description |
|-------------|----------------|------|-------------|
| `/api/v1/users/*` | User Service | 21501 | User management & authentication |
| `/api/v1/settings/*` | Settings Service | 21502 | System configuration |
| `/api/v1/zones/*` | Zone Service | 21503 | Zone & center management |
| `/api/v1/orders/*` | Order Service | 21504 | Order management (future) |

### 3. User Context Management

- Extracts user information from JWT tokens
- Makes user context available via `UserContext` class
- Passes user context to downstream services

### 4. CORS Configuration

- Configurable allowed origins
- Supports multiple frontend applications
- Pre-flight request handling

## API Endpoints

### Health Check

```bash
GET /api/v1/health
```

Response:
```json
{
  "status": "UP",
  "message": "API Gateway is running"
}
```

### Authentication Routes

All authentication routes are public (no JWT required).

#### Login

```bash
POST /api/v1/auth/login
Content-Type: application/json

{
  "username": "admin",
  "password": "password",
  "type": "BACKEND"
}
```

Response:
```json
{
  "result": {
    "message": "Login successful",
    "access_token": "eyJhbGc...",
    "expires_in": 300,
    "refresh_expires_in": 1800,
    "refresh_token": "eyJhbGc...",
    "token_type": "Bearer",
    "user": {
      "id": "uuid",
      "keycloakId": "keycloak-uuid",
      "username": "admin",
      "email": "admin@delivery-system.com",
      "firstName": "System",
      "lastName": "Administrator",
      "phone": null,
      "address": null,
      "identityNumber": null,
      "status": "ACTIVE",
      "createdAt": "2024-01-01T00:00:00",
      "updatedAt": null
    }
  },
  "message": "Login successful"
}
```

Login types:
- `BACKEND`: Admin/Staff (uses backend realm)
- `FRONTEND`: Shipper/Client (uses client realm)

#### Validate Token

```bash
POST /api/v1/auth/validate-token
Authorization: Bearer <token>
```

#### Refresh Token

```bash
POST /api/v1/auth/refresh-token
Content-Type: application/json

{
  "refreshToken": "eyJhbGc..."
}
```

#### Get Current User

```bash
GET /api/v1/auth/me
Authorization: Bearer <token>
```

#### Logout

```bash
POST /api/v1/auth/logout
Content-Type: application/json

{
  "refreshToken": "eyJhbGc..."
}
```

#### Sync User

```bash
POST /api/v1/auth/sync
Authorization: Bearer <token>
```

Syncs the authenticated Keycloak user to the User Service database.

### User Management Routes (Proxied)

All user routes require authentication.

```bash
# Get all users (paginated)
GET /api/v1/users?page=0&size=10

# Get user by ID
GET /api/v1/users/{id}

# Create user
POST /api/v1/users

# Update user
PUT /api/v1/users/{id}

# Delete user
DELETE /api/v1/users/{id}
```

See [User Service Documentation](../User_service/README.MD) for detailed request/response formats.

### Settings Routes (Proxied)

```bash
# Get all settings (requires auth)
GET /api/v1/settings

# Get public settings (public route)
GET /api/v1/settings/public

# Get setting by key (requires auth)
GET /api/v1/settings/{key}

# Get setting value only (requires auth)
GET /api/v1/settings/{key}/value

# Create setting (requires auth)
POST /api/v1/settings

# Update setting (requires auth)
PUT /api/v1/settings/{key}

# Delete setting (requires auth)
DELETE /api/v1/settings/{key}
```

See [Settings Service Documentation](../Settings_service/README.md) for detailed request/response formats.

### Zone Routes (Proxied)

```bash
# Get all zones (requires auth)
GET /api/v1/zones?page=0&size=10

# Get zone by ID (requires auth)
GET /api/v1/zones/{id}

# Get zones by center (requires auth)
GET /api/v1/zones/center/{centerId}

# Create zone (requires auth)
POST /api/v1/zones

# Update zone (requires auth)
PUT /api/v1/zones/{id}

# Delete zone (requires auth)
DELETE /api/v1/zones/{id}
```

See [Zone Service Documentation](../zone_service/README.md) for detailed request/response formats.

## Security

### JWT Token Validation

The gateway validates JWT tokens from Keycloak:

1. Extracts token from `Authorization: Bearer <token>` header
2. Validates token signature using Keycloak public key
3. Checks token expiration
4. Extracts user information (username, roles, email)
5. Makes user context available to controllers

### Public Routes

Routes annotated with `@PublicRoute` bypass authentication:

- `/api/v1/health`
- `/api/v1/auth/login`
- `/api/v1/auth/validate-token`
- `/api/v1/auth/refresh-token`
- `/api/v1/auth/logout`
- `/api/v1/settings/public`

### Protected Routes

All other routes require a valid JWT token in the `Authorization` header.

## Response Format

All responses follow the `BaseResponse<T>` contract:

### Success Response

```json
{
  "result": { ... },
  "message": "Operation successful"
}
```

### Error Response

```json
{
  "message": "Error description",
  "errors": [
    {
      "field": "fieldName",
      "message": "Validation error message"
    }
  ]
}
```

### HTTP Status Codes

- `200 OK`: Successful operation
- `201 Created`: Resource created
- `400 Bad Request`: Validation error or bad input
- `401 Unauthorized`: Authentication required or invalid token
- `403 Forbidden`: Insufficient permissions
- `404 Not Found`: Resource not found
- `500 Internal Server Error`: Server error
- `503 Service Unavailable`: Backend service unavailable

## Development

### Prerequisites

- Java 17+
- Maven 3.6+
- Keycloak running on port 8080
- Backend services (User, Settings, Zone) running

### Run Application

```bash
cd BE/api-gateway
mvn spring-boot:run
```

### Build Application

```bash
mvn clean package
```

### Run Tests

```bash
mvn test
```

### Access Swagger UI

```
http://localhost:21500/swagger-ui.html
```

## Integration with Backend Services

### WebClient Configuration

The gateway uses Spring WebFlux WebClient for non-blocking HTTP calls to backend services:

```java
@Bean
public WebClient userServiceClient() {
    return WebClient.builder()
        .baseUrl(userServiceUrl)
        .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
        .build();
}
```

### Error Handling

The gateway handles backend service errors:

- Connection timeouts
- Service unavailable (503)
- Invalid responses
- Network errors

Errors are transformed into appropriate HTTP responses with meaningful messages.

## Keycloak Configuration

### Required Realms

1. **Backend Realm** (`delivery-system-backend`)
   - For admin and staff users
   - Client: `delivery-backend`
   - Used for: Management web app, admin operations

2. **Client Realm** (`delivery-system-client`)
   - For shipper and customer users
   - Clients: `delivery-management-web`, `delivery-mobile-app`
   - Used for: Mobile app, customer portal

### Client Configuration

Each client must be configured with:
- **Access Type**: `confidential` or `public`
- **Standard Flow**: Enabled
- **Direct Access Grants**: Enabled
- **Valid Redirect URIs**: Configured for each frontend
- **Web Origins**: Configured for CORS

See [Keycloak Migration Guide](./KEYCLOAK_MIGRATION.md) for detailed setup instructions.

## Docker Support

### Build Docker Image

```bash
docker build -t api-gateway .
```

### Run Container

```bash
docker run -p 21500:21500 \
  --env-file ../env.local \
  api-gateway
```

### Docker Compose

The gateway is included in the main `docker-compose.yml` at project root.

```bash
# Start all services
docker-compose up -d

# View logs
docker-compose logs -f api-gateway
```

## Monitoring & Logging

### Health Checks

```bash
# Basic health check
curl http://localhost:21500/api/v1/health

# Actuator endpoints (if enabled)
curl http://localhost:21500/actuator/health
curl http://localhost:21500/actuator/info
```

### Logging

Logs include:
- Incoming requests (URL, method, user)
- Outgoing proxy requests (target service, latency)
- Authentication events (login, logout, token validation)
- Errors and exceptions

Log level can be configured in `application.yaml`.

## Best Practices

1. **Always use the gateway** for client requests in production
2. **Avoid direct service calls** from clients (use gateway routing)
3. **Use public routes sparingly** - only for truly public endpoints
4. **Include JWT token** in Authorization header for protected routes
5. **Handle token expiration** - implement token refresh logic in clients
6. **Check user roles** in backend services for fine-grained authorization
7. **Use standard HTTP status codes** for consistent error handling

## Troubleshooting

### Cannot connect to Keycloak

- Verify Keycloak is running on the configured host/port
- Check `KEYCLOAK_HOST` and `KEYCLOAK_PORT` in env.local
- Ensure network connectivity
- Check Keycloak logs for errors

### Backend service unavailable

- Verify the target service is running
- Check service URL configuration
- Verify network connectivity between services
- Check backend service health endpoint

### Token validation fails

- Verify token is not expired
- Check Keycloak realm configuration
- Ensure client configuration matches
- Verify token is in correct format: `Bearer <token>`

### CORS errors

- Add frontend origin to `CORS_ALLOWED_ORIGINS`
- Verify CORS configuration in `CorsConfig.java`
- Check browser console for specific CORS error

## Documentation

- [API Documentation](.docs/README.md)
- [Route Documentation](.docs/route/) - [auth.md](.docs/route/auth.md), [users.md](.docs/route/users.md), [zones.md](.docs/route/zones.md), [centers.md](.docs/route/centers.md), [addresses.md](.docs/route/addresses.md), [routing.md](.docs/route/routing.md), [osrm.md](.docs/route/osrm.md), [parcels.md](.docs/route/parcels.md), [sessions.md](.docs/route/sessions.md), [assignments.md](.docs/route/assignments.md), [settings.md](.docs/route/settings.md)
- [Keycloak Migration Guide](./KEYCLOAK_MIGRATION.md)
- [Global RESTful Standards](../RESTFUL.md)

## Summary

The API Gateway provides:
- Single entry point for all client requests
- Centralized authentication & authorization via Keycloak
- Request routing and proxying to backend microservices
- JWT token validation and user context management
- CORS configuration for frontend applications
- Standardized error handling
- API documentation

**Port**: 21500
**Package**: com.ds.gateway
**Main Class**: `GatewayApplication.java`
